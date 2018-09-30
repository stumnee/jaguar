package controllers

import javax.inject.Inject

import io.swagger.annotations._
import models._
import play.api.mvc._

import scala.concurrent.Future
import models.UserJsonFormats._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import TokenJsonFormats._


@Api(value = "/users")
class UserController @Inject()(cc: ControllerComponents, val userRepository: UserRepository, tokenRepository: TokenRepository) extends AbstractController(cc) with UserValidation {


  def list() = Action.async { implicit req =>
    userRepository.getAll().map { users =>
      Ok(Json.toJson(users))
    }
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "The User to add, in Json Format", required = true, dataType = "models.UserDao", paramType = "body")
  ))
  def create() = Action.async(parse.json){ req =>
    req.body.validate[UserDao].map{ event =>
      userRepository.add(event).map{ _ =>
        Created
      }
    }.getOrElse(Future.successful(BadRequest("Invalid User format")))
  }

  def createToken(username: String) = withUserValidation { req =>
      tokenRepository.create(username).map{results=>
        Created(Json.toJson(results.get))
      }
  }

  def listTokens(username: String) = withUserValidation { req =>
    tokenRepository.getUserTokens(username).map{ tokens =>
      Ok(Json.toJson(tokens))
    }
  }

  def deleteToken(username: String, token: String) = withUserValidation { req =>

    tokenRepository.delete(username, token).map {
      case Some(t) => Ok(Json.toJson(t))
      case None => NotFound
    }
  }

  def updateToken(username: String, token: String) = withUserValidation { req =>

    val action:String = req.queryString.getOrElse("action", "").toString

    action.match {
      case "revoke" => tokenRepository.revoke(username, token).map {
                          case Some(t) => Ok(Json.toJson(t))
                          case None => NotFound
                        }

      case "unrevoke" => tokenRepository.unrevoke(username, token).map {
                          case Some(t) => Ok(Json.toJson(t))
                          case None => NotFound
                        }
    }

  }

  @ApiOperation(
    value = "Update a User",
    response = classOf[UserDao]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid User format")
  )
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "The updated User, in Json Format", required = true, dataType = "models.UserDao", paramType = "body")
  )
  )
  def update(@ApiParam(value = "The id of the Event to update")
             username: String) = Action.async(parse.json){ req =>
    req.body.validate[UserDao].map{ user =>
      userRepository.update(username, user).map {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Json")))
  }


  @ApiOperation(
    value = "Delete an User",
    response = classOf[UserDao]
  )
  def delete(@ApiParam(value = "The username of the User to delete") username: String) = Action.async{ req =>
    userRepository.delete(username).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
  }
}


trait UserValidation { self: AbstractController =>

  @Inject() val userRepository: UserRepository

  def withUserValidation(f: => Request[AnyContent] => Future[Result]) = Action.async { implicit request =>

    val uriPattern = "/users/([^\\/]+)/token".r
    val username = uriPattern.findFirstMatchIn(request.uri).get

    userRepository.getByUsername(username.group(1)) flatMap { userOption: Option[User] =>
      userOption match {
        case Some(_) => f(request)
        case None => Future.successful(NotFound("Invalid username"))
      }
    }
  }

}
