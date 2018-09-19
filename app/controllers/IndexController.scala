package controllers

import javax.inject._

import actors._
import akka.actor.ActorSystem
import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import models.TokenRepository
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.concurrent.Future

@Api(value = "/index")
class IndexController @Inject()(
                               val tokenRepository: TokenRepository,
      cc: ControllerComponents)(
      implicit val system: ActorSystem,
      implicit val materializer: akka.stream.Materializer
  ) extends AbstractController(cc) with TokenAuthentication  {



  def index() = Action { implicit request: Request[AnyContent] =>
    request.session.get("user").map { user =>
      Ok(views.html.index(user))
    }.getOrElse {
      Ok(views.html.index(null))
    }
  }

  def testWebsocket() = Action { request =>
    Ok(views.html.testWebsocket())
  }

  @ApiOperation(
    value = "Test API Token",
    response = classOf[Void],
    code = 200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid API Token")
  )
  )
  def testToken = withAPIToken { _ =>
    Ok("Token Validated")
  }

  def ws = WebSocket.acceptOrResult[String, String] { request =>
    Future.successful(request.session.get("user") match {
      case None => Left(Forbidden("Must log in first"))
      case Some(_) => Right(
        ActorFlow.actorRef { out =>
          MyWebSocketActor.props(out)
        }
      )
    })

  }
//
//  def ws = WebSocket.accept[String, String] { request =>
//    ActorFlow.actorRef { out =>
//      MyWebSocketActor.props(out)
//    }
//  }
}