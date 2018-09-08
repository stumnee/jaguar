package controllers

import javax.inject.Inject

import io.swagger.annotations._
import models.{Event, UserDao, UserRepository}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future
import models.UserJsonFormats._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global


@Api(value = "/users")
class UserController @Inject()(cc: ControllerComponents, userRepository: UserRepository) extends AbstractController(cc) {
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

  @ApiOperation(
    value = "Delete an User",
    response = classOf[UserDao]
  )
  def delete(@ApiParam(value = "The id of the Event to delete") username: String) = Action.async{ req =>
    userRepository.delete(username).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
  }
}
