import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection
import models.Event
import models.JsonFormats._
import org.joda.time.DateTime
import play.api.test.FakeRequest
import play.api.test.Helpers._

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

  "Get all events" in {
    val Some(result) = route(app, FakeRequest(GET, "/events"))
    var resultList = contentAsJson(result).as[List[Event]]
    resultList.length mustEqual 1
    status(result) mustBe OK
  }
}
