package controllers

import javax.inject.Inject

import models.UserRepository
import org.mindrot.jbcrypt.BCrypt
import play.api.data.FormError
import play.api.mvc.{AnyContent, MessagesAbstractController, MessagesControllerComponents, MessagesRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoginController @Inject()(cc: MessagesControllerComponents, userRepository: UserRepository) extends MessagesAbstractController(cc) {
  import LoginForm._


  def testLogin() = Action { implicit  request: MessagesRequest[AnyContent] =>
    request.session.get("user").map { user =>
      Ok(views.html.index(user.toString))
    }.getOrElse {
      Ok(views.html.testLogin(form))
    }

  }

  def testLoginSubmit() = Action.async { implicit request:MessagesRequest[AnyContent] =>


    //println(BCrypt.hashpw("badpass", BCrypt.gensalt()))
    form.bindFromRequest.fold(formWithErrors => {
      Future {
        BadRequest(views.html.testLogin(formWithErrors))
      }
    }, userData => {
      userRepository.getByUsername(userData.username).map {
          case Some(user) => if (BCrypt.checkpw(userData.password, user.password)) {
            Redirect(routes.IndexController.index()).withSession(
              "user" -> userData.username)
          } else {
            BadRequest(views.html.
              testLogin(form.fill(userData).
                withError(FormError("error", "Invalid username or password"))))
          }
          case None => BadRequest(views.html.
            testLogin(form.fill(userData).
              withError(FormError("error", "Invalid username or password"))))
        }
      }
    )

  }

  def testLogout() = Action { implicit request =>
    Redirect(routes.LoginController.testLogin()).withNewSession
  }

}
