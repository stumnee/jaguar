import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection
import models.Event
import models.JsonFormats._
import org.joda.time.DateTime
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.Json
import reactivemongo.play.json._
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventSpec extends PlayWithMongoSpec with BeforeAndAfter {
  var events: Future[JSONCollection] = _

  before {
    // Init DB
    await {
      events = reactiveMongoApi.database.map(_.collection("events"))

      events.flatMap(_.insert[Event](ordered = false).many(List(
        Event(id = Some("1234"), tags = Seq("tag1"), Seq(), title = "title 1", data = "data 1", createdTime = Some(new DateTime()))
      )))
    }
  }

  after {
    events.flatMap(_.drop(failIfNotFound = false))
  }

  "Get all Events" in {
    val Some(result) = route(app, FakeRequest(GET, "/events"))
    val resultList = contentAsJson(result).as[List[Event]]
    resultList.length mustEqual 1
    status(result) mustBe OK
  }

  "Add an Event" in {
    val payload = Event(id = Some("123456"), tags = Seq("tagNew"), Seq(), title = "New Title", data = "data new", createdTime = Some(new DateTime()))
    val Some(result) = route(app, FakeRequest(POST, "/events").withJsonBody(Json.toJson(payload)))
    status(result) mustBe CREATED
  }

  "Delete an Event" in {
    val query = BSONDocument()
    val Some(eventToDelete) = await(events.flatMap(_.find(query).one[Event]))

    val eventIdToDelete = eventToDelete.id.get
    val Some(result) = route(app, FakeRequest(DELETE, s"/events/$eventIdToDelete"))
    status(result) mustBe OK
  }

  "Update an Event" in {
    val query = BSONDocument()
    val payload = Json.obj(
      "tags" -> Seq("tag1", "tag2"),
      "data" -> "data",
      "title" -> "Event title updated"
    )
    val Some(eventToUpdate) = await(events.flatMap(_.find(query).one[Event]))
    val eventIdToUpdate = eventToUpdate.id.get
    val Some(result) = route(app, FakeRequest(PATCH, s"/events/$eventIdToUpdate").withJsonBody(Json.toJson(payload)))
    val updatedEvent = contentAsJson(result).as[Event]
    updatedEvent.title mustEqual "Event title updated"
    status(result) mustBe OK
  }
}
