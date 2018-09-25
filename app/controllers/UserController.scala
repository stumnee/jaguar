package controllers

import javax.inject.Inject

import io.swagger.annotations._
import models._
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future
import models.UserJsonFormats._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import TokenJsonFormats._


@Api(value = "/users")
class UserController @Inject()(cc: ControllerComponents, userRepository: UserRepository, tokenRepository: TokenRepository) extends AbstractController(cc) {


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

  def createToken(username: String) = Action.async { req =>

    userRepository.getByUsername(username) flatMap { userOption: Option[User] =>
      userOption.map{user=>
          tokenRepository.create(user.username).map{results=>
            Created(Json.toJson(results.get))
          }
      }.getOrElse(Future.successful(Ok("")))
    }
  }

  def deleteToken(username: String, token: String) = Action.async { req =>

    tokenRepository.delete(username, token).map {
      case Some(t) => Ok(Json.toJson(t))
      case None => NotFound
    }
  }

  def revokeToken(username: String, token: String) = Action.async { req =>
    tokenRepository.revoke(username, token).map {
      case Some(t) => Ok(Json.toJson(t))
      case None => NotFound
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
