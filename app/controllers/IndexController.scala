package controllers

import javax.inject._

import actors._
import akka.actor.ActorSystem
import play.api.libs.streams.ActorFlow
import play.api.mvc._

class IndexController @Inject()(
      cc: ControllerComponents)(
      implicit val system: ActorSystem,
      implicit val materializer: akka.stream.Materializer
  ) extends AbstractController(cc) {



  def index() = Action { implicit request: Request[AnyContent] =>
    request.session.get("user").map { user =>
      Ok(views.html.index(user))
    }.getOrElse {
      Ok(views.html.index(null))
    }
  }

  def ws = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      MyWebSocketActor.props(out)
    }
  }
}