package controllers

import javax.inject.Inject

import io.swagger.annotations._
import models.JsonFormats._
import models.{Event, EventDao, EventRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api(value = "/events")
class EventController @Inject()(cc: ControllerComponents, eventRepository: EventRepository) extends AbstractController(cc) {

  @ApiOperation(
    value = "Find all Events",
    response = classOf[Event],
    responseContainer = "List"
  )
  def getAll = Action.async { implicit request =>
    eventRepository.getAll(request.queryString.get("sort")).map{ events =>
      Ok(Json.toJson(events))
    }
  }


  @ApiOperation(
    value = "Get an Event",
    response = classOf[Event]
  )
  @ApiResponses(Array(
      new ApiResponse(code = 404, message = "Event not found")
    )
  )
  def get(@ApiParam(value = "The id of the Event to fetch") eventId: BSONObjectID) = Action.async{ req =>
    eventRepository.get(eventId).map{ maybeEvent =>
      maybeEvent.map{ event =>
        Ok(Json.toJson(event))
      }.getOrElse(NotFound)
    }
  }

  @ApiOperation(
    value = "Add a new Event to the list",
    response = classOf[Void],
    code = 201
  )
  @ApiResponses(Array(
      new ApiResponse(code = 400, message = "Invalid Event format")
    )
  )
  @ApiImplicitParams(Array(
      new ApiImplicitParam(value = "The Event to add, in Json Format", required = true, dataType = "models.Event", paramType = "body")
    )
  )
  def create() = Action.async(parse.json){ req =>
    req.body.validate[EventDao].map{ event =>
      eventRepository.add(event).map{ _ =>
        Created
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Event format")))
  }

  @ApiOperation(
    value = "Update a Event",
    response = classOf[Event]
  )
  @ApiResponses(Array(
      new ApiResponse(code = 400, message = "Invalid Event format")
    )
  )
  @ApiImplicitParams(Array(
      new ApiImplicitParam(value = "The updated Event, in Json Format", required = true, dataType = "models.Event", paramType = "body")
    )
  )
  def update(@ApiParam(value = "The id of the Event to update")
                 eventId: BSONObjectID) = Action.async(parse.json){ req =>
    req.body.validate[Event].map{ event =>
      eventRepository.update(eventId, event).map {
        case Some(event) => Ok(Json.toJson(event))
        case None => NotFound
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Json")))
  }

  @ApiOperation(
    value = "Delete an Event",
    response = classOf[Event]
  )
  def delete(@ApiParam(value = "The id of the Event to delete") eventId: BSONObjectID) = Action.async{ req =>
    eventRepository.delete(eventId).map {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }
  }

}
