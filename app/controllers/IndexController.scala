package controllers

import java.nio.file.Paths
import javax.inject._

import play.api._
import play.api.mvc._

@Singleton
class IndexController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def testUpload() = Action { implicit  request: Request[AnyContent] =>
    Ok(views.html.testUpload())
  }

  def testUploadSubmit = Action(parse.multipartFormData) { request =>
    request.body.file("testFile").map { testFile=>
      val filename = Paths.get(testFile.filename).getFileName

      testFile.ref.moveTo(Paths.get(s"/tmp/$filename"), replace = true)
      Ok("File Uploaded")
    }.getOrElse {
      Redirect(routes.IndexController.testUpload)
    }
  }
}
