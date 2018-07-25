package controllers

import java.nio.file.Paths
import javax.inject.Inject

import actors._

import akka.NotUsed
import akka.util.Timeout

import scala.util.Failure
import org.joda.time.DateTime

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
import reactivemongo.play.json.collection._

class IndexController @Inject() (
      @Named("userParentActor") userParentActor: ActorRef,
      cc: ControllerComponents,
      val reactiveMongoApi: ReactiveMongoApi,
      implicit val materializer: akka.stream.Materializer
  ) extends AbstractController(cc) with MongoController with ReactiveMongoComponents with SameOriginCheck {

  import java.util.UUID
  import MongoController.readFileReads

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

  def ws: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] {
    case rh if sameOriginCheck(rh) =>
      wsFutureFlow(rh).map { flow =>
        Right(flow)
      }.recover {
        case e: Exception =>
          logger.error("Cannot create websocket", e)
          val jsError = Json.obj("error" -> "Cannot create websocket")
          val result = InternalServerError(jsError)
          Left(result)
      }

    case rejected =>
      logger.error(s"Request ${rejected} failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }

  private def wsFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    // Use guice assisted injection to instantiate and configure the child actor.
    implicit val timeout = Timeout(1.second) // the first run in dev can take a while :-(
    val future: Future[Any] = userParentActor ? UserParentActor.Create(request.id.toString)
    val futureFlow: Future[Flow[JsValue, JsValue, NotUsed]] = future.mapTo[Flow[JsValue, JsValue, NotUsed]]
    futureFlow
  }
}


trait SameOriginCheck {

  def logger: Logger

  /**
    * Checks that the WebSocket comes from the same origin.  This is necessary to protect
    * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
    *
    * See https://tools.ietf.org/html/rfc6455#section-1.3 and
    * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
    */
  def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  /**
    * Returns true if the value of the Origin header contains an acceptable value.
    *
    * This is probably better done through configuration same as the allowedhosts filter.
    */
  def originMatches(origin: String): Boolean = {
    origin.contains("localhost:9000") || origin.contains("localhost:19001")
  }

}
