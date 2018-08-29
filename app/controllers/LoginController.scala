package controllers

import javax.inject.Inject

import akka.actor.FSM
import controllers.LoginForm.form
import models.{UserFormModel, UserRepository}
import play.api.data.{Form, FormError}
import play.api.mvc.{AnyContent, MessagesAbstractController, MessagesControllerComponents, MessagesRequest}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoginController @Inject()(cc: MessagesControllerComponents, userRepository: UserRepository) extends MessagesAbstractController(cc) {



  def testLogin() = Action { implicit  request: MessagesRequest[AnyContent] =>
    request.session.get("user").map { user =>
      Ok(views.html.index(user.toString))
    }.getOrElse {
      Ok(views.html.testLogin(form))
    }

  }

  def testLoginSubmit() = Action.async { implicit request:MessagesRequest[AnyContent] =>
//    val errFunc = { formWithErrors: Form[UserFormModel] =>
//      BadRequest(views.html.testLogin(formWithErrors))
//    }
//
//    val successFunc = { data: UserFormModel =>
//      val usersFuture = userRepository.getByUsername(data.username)
//
//      usersFuture onComplete {
//        case Success(userOption) => userOption match {
//          case Some(user) => Redirect(routes.IndexController.index()).withSession(
//            "user" -> data.username)
//          case None => BadRequest(views.html.testLogin(LoginForm[UserFormModel]))
//        }
//        case Failure(t) => println("Error", t.getMessage)
//                            BadRequest(views.html.testLogin(LoginForm[UserFormModel]))
//      }
//    }

    form.bindFromRequest.fold(formWithErrors => {
      Future {
        BadRequest(views.html.testLogin(formWithErrors))
      }
    }, userData => {
      userRepository.getByUsername(userData.username).map { userOption =>
        userOption match {
          case Some(user) => Redirect(routes.IndexController.index()).withSession(
                      "user" -> userData.username)
          case None => BadRequest(views.html.testLogin(form.withError(FormError("error", "Invalid username or password"))))
        }
      }
    })

  }

  def testLogout() = Action { implicit request =>
    Redirect(routes.LoginController.testLogin()).withNewSession
  }

}
