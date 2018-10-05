import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import models.{User}
import models.UserJsonFormats._
import reactivemongo.play.json._
import play.api.libs.json.{JsObject, JsString}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global

class UserSpec extends PlayWithMongoSpec with BeforeAndAfter {
  var users: Future[JSONCollection] = _
  var userJane: User = _
  var userTom: User = _

  before {
    await {
      userJane = User(_id = None, username = "Jane", password = "badpass")
      userTom = User(_id = None, username = "Tom", password = "badpass")

      users = reactiveMongoApi.database.map(_.collection("users"))

      users.flatMap(_.insert[User](ordered = false).many(List(
        userJane,
        userTom
      )))

    }
  }

  "Get all Users" in {
    val Some(result) = route(app, FakeRequest(GET, "/users"))
    val resultList = contentAsJson(result).as[List[User]]
    resultList.length mustEqual 2
    status(result) mustBe OK
  }

  "Add an User" in {
    val Some(result) = route(app, FakeRequest(POST, "/users").withJsonBody(JsObject(List("username"->JsString("Drew"), "password"->JsString("Breeze")))))
    status(result) mustBe CREATED
  }

  "Delete a Non User" in {
    val Some(result) = route(app, FakeRequest(DELETE, "/users/nonExistentUser"))
    status(result) mustBe NOT_FOUND
  }

  "Delete an User" in {
    val Some(result) = route(app, FakeRequest(DELETE, "/users/" + userJane.username))
    status(result) mustBe OK
  }

  "Update an User" in {
    val payload = JsObject(List("_id"->JsString("id"), "username"->JsString("updated username"), "password"->JsString("updated password")))
    val query = BSONDocument()

    val Some(result) = route(app, FakeRequest(PATCH, "/users/" + userJane.username).withJsonBody(payload))

    status(result) mustBe OK
  }

  after {
    users.flatMap(_.drop(failIfNotFound = false))
  }
}
