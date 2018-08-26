package controllers

import javax.inject.Inject

import controllers.LoginForm.form
import models.UserFormModel
import play.api.data.Form
import play.api.mvc.{AnyContent, MessagesAbstractController, MessagesControllerComponents, MessagesRequest}

class LoginController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {



  def testLogin() = Action { implicit  request: MessagesRequest[AnyContent] =>
    request.session.get("user").map { user =>
      Ok(views.html.index(user.toString))
    }.getOrElse {
      Ok(views.html.testLogin(form))
    }

  }

  def testLoginSubmit() = Action { implicit request:MessagesRequest[AnyContent] =>
    val errFunc = { formWithErrors: Form[UserFormModel] =>
      BadRequest(views.html.testLogin(formWithErrors))
    }

    val successFunc = { data: UserFormModel =>
      Redirect(routes.IndexController.index()).withSession(
        "user" -> data.username)
    }

    val formValidationResult = form.bindFromRequest

    formValidationResult.fold(errFunc, successFunc)

  }

  def testLogout() = Action { implicit request =>
    Redirect(routes.LoginController.testLogin()).withNewSession
  }

}
