package controllers

import javax.inject.Inject

import models.{Token, TokenRepository}
import org.joda.time.DateTime
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait TokenAuthentication { self: AbstractController =>

  @Inject() val tokenRepository: TokenRepository

  def validateToken(token: String): Future[Option[Token]] = {
    tokenRepository.getToken(token)
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
  def withAPIToken(f: => Request[AnyContent] => Result) = Action.async { implicit request =>
    val authToken = request.headers.get("Authorization") flatMap { authHeaderToken =>
      extractToken(authHeaderToken)
    }

    authToken match {
      case Some(t) => validateToken(t) map {
          case Some(token) => if (new DateTime().isAfter(token.expiry))
            Unauthorized("API token expired")
          else
            token.revoked.fold(f(request)) {_ => Unauthorized("API token revoked") }
          case _ => Unauthorized("Invalid API token")
        }
      case _ => Future.successful(Unauthorized("No valid API token"))
    }
  }

}
