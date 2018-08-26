package controllers

import models.UserFormModel
import play.api.data.Form
import play.api.data.Forms._

object LoginForm {


  val form = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserFormModel.apply)(UserFormModel.unapply)
  )
}
