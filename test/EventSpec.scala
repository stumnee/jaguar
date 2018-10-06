import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection
import models.Event
import models.EventJsonFormats._
import org.joda.time.DateTime
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.Json
import reactivemongo.play.json._
import reactivemongo.bson.{BSONDocument, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventSpec extends PlayWithMongoSpec with BeforeAndAfter {
  var events: Future[JSONCollection] = _
  var event1: Event = _
  var event2: Event = _
  var event3: Event = _

  before {
    // Init DB
    await {
      events = reactiveMongoApi.database.map(_.collection("events"))

      event1 = Event(id = Some(BSONObjectID.generate().toString()), tags = Seq("tag1"), Seq(), title = "title 1", data = "data 1", createdTime = Some(new DateTime()))
      event2 = Event(id = Some(BSONObjectID.generate().toString()), tags = Seq("tag2"), Seq(), title = "title 2", data = "data 2", createdTime = Some(new DateTime()))
      event3 = Event(id = Some(BSONObjectID.generate().toString()), tags = Seq("tag3"), Seq(), title = "title 3", data = "data 3", createdTime = Some(new DateTime()))
      events.flatMap(_.insert[Event](ordered = false).many(List(
        event1,
        event2,
        event3
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

  "Get all events created later than 2018-01-01" in {
    val Some(result) = route(app, FakeRequest(GET, "/events?created>2018-01-01"))
    val resultList = contentAsJson(result).as[List[Event]]
    status(result) mustBe OK
    resultList.filter(_.createdTime > new DateTime("2018-01-01")).length == resultList.length
  }

  "Get all events created equal to certain date" in {
    val Some(result) = route(app, FakeRequest(GET, s"/events?created>${event1.createdTime.toString}"))
    val resultList = contentAsJson(result).as[List[Event]]
    status(result) mustBe OK
    resultList.length > 1 mustBe true
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
