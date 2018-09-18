package controllers

import javax.inject.Inject

import models.{Token, TokenRepository}
import play.api.mvc._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


trait TokenAuthenication { self: AbstractController =>

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
    val token = request.headers.get("Authorization") flatMap { authHeaderToken =>
      extractToken(authHeaderToken)
    }

    token match {
      case Some(t) => validateToken(t) map {
          case Some(_) => f(request)
          case _ => Unauthorized("Invalid API token")
        }
      case _ => Future.successful(Unauthorized("No valid API token"))
    }
  }

}
