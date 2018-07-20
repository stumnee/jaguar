package controllers

import java.nio.file.Paths
import javax.inject._

import play.api._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.libs.json.{JsString, Json}
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi}
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import reactivemongo.play.json.JSONSerializationPack

import scala.concurrent.duration.Duration
import play.api.mvc.BaseController

import scala.concurrent.{Await, Future}
import scala.util.Failure

@Singleton
class IndexController @Inject() (
      cc: ControllerComponents,
      val reactiveMongoApi: ReactiveMongoApi,
      implicit val materializer: akka.stream.Materializer
  ) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  implicit def ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def testUpload() = Action { implicit  request: Request[AnyContent] =>
    Ok(views.html.testUpload())
  }

  def testUploadSubmitFile = Action(parse.multipartFormData) { request =>
    request.body.file("te stFile").map { testFile=>
      val filename = Paths.get(testFile.filename).getFileName

      testFile.ref.moveTo(Paths.get(s"/tmp/$filename"), replace = true)
      Ok("File Uploaded")
    }.getOrElse {
      Redirect(routes.IndexController.testUpload)
    }
  }

  private def gridFS: Future[MongoController.JsGridFS] = for {
    db <- reactiveMongoApi.database
    fs = GridFS[JSONSerializationPack.type](db)
    _ <- fs.ensureIndex().map { index =>
      // let's build an index on our gridfs chunks collection if none
      Logger.info(s"Checked index, result is $index")
    }
  } yield fs

  def testUploadSubmit() = {
    lazy val fs = Await.result(gridFS, Duration("5s"))

    Action.async(gridFSBodyParser(fs)) { request=>
      val futureFile = request.body.files.head.ref.andThen {
        case Failure(cause) => Logger.error("Fails to save file", cause)
      }

      val futureUpdate = for {
        file <- futureFile
      } yield Redirect(routes.IndexController.testUpload)

      futureUpdate.recover {
        case e => InternalServerError(e.getMessage())
      }

    }



  }
}
