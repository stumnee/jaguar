package controllers

import java.nio.file.Paths
import javax.inject._

import akka.actor.ActorRef
import akka.NotUsed
import akka.util.Timeout

import scala.util.Failure
import org.joda.time.DateTime
//import actors._

import scala.concurrent.{Await, Future, duration}
import duration.Duration
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json._
import akka.actor.ActorSystem
import reactivemongo.play.json.collection._
import play.api.libs.streams.ActorFlow
import actors._

class IndexController @Inject() (
      cc: ControllerComponents)(
      val reactiveMongoApi: ReactiveMongoApi,
      implicit val system: ActorSystem,
      implicit val materializer: akka.stream.Materializer
  ) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  import java.util.UUID
  import MongoController.readFileReads

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  implicit def ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }


  def testLogin() = Action { implicit  request: Request[AnyContent] =>
    request.session.get("connected").map { user =>
      Ok("Hello " + user)
    }.getOrElse {
      Ok(views.html.testLogin())
    }

  }

  def testLoginSubmit() = Action { implicit request =>
    Ok("Welcome!").withSession(
      "connected" -> "user@gmail.com")
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

      println(      futureFile)

      val futureUpdate = for {
        file <- futureFile

//        updateResult <- fs.files.update(
//          Json.obj("_id" -> file.id),
//          Json.obj("$set" -> Json.obj("article" -> 2)))
      } yield Ok(Json.toJson(Map("id"->file.id)))//Redirect(routes.IndexController.testUpload)

      //Ok(Json.toJson(futureUpdate))
//      futureUpdate.onComplete{
//        _ => Ok()
//      }

      futureUpdate.recover {
        case e => InternalServerError(e.getMessage())
      }

    }
  }

  def ws = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      MyWebSocketActor.props(out)
    }
  }
}