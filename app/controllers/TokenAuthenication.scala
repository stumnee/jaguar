package controllers

import play.api.mvc._

trait TokenAuthenication { self: AbstractController =>

  def validateToken(token: String): Option[Boolean] = {
    if (token == "123")
      Some(true)
    else
      None
  }

  def extractToken(authHeader: String): Option[String] = {
    authHeader.split("Token token=") match {
      case Array(_, token) => Some(token)
      case _               => None
    }
  }

  /**
    * curl -i http://localhost:9000/testToken -H "Authorization: Token token=123"
    * @param f
    * @return
    */
  def withAPIToken(f: => Request[AnyContent] => Result) = Action { implicit request =>
    request.headers.get("Authorization") flatMap { authHeaderToken =>
      extractToken(authHeaderToken) flatMap { token =>
        validateToken(token) flatMap { _ =>
          Some(f(request))
        }
      }
    } getOrElse Unauthorized("Invalid API token")
  }

}
