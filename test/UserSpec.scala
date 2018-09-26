import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import models.{Token, User}
import models.UserJsonFormats._
import models.TokenJsonFormats._
import reactivemongo.play.json._
import play.api.libs.json.{JsObject, JsString}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global

class UserSpec extends PlayWithMongoSpec with BeforeAndAfter {
  var users: Future[JSONCollection] = _

  before {
    await {
      users = reactiveMongoApi.database.map(_.collection("users"))

      users.flatMap(_.insert[User](ordered = false).many(List(
        User(_id = None, username = "Jane", password = "badpass")
      )))
    }
  }

  "Get all Users" in {
    val Some(result) = route(app, FakeRequest(GET, "/users"))
    val resultList = contentAsJson(result).as[List[User]]
    resultList.length mustEqual 1
    status(result) mustBe OK
  }

  "Add an User" in {
    val Some(result) = route(app, FakeRequest(POST, "/users").withJsonBody(JsObject(List("username"->JsString("drew"), "password"->JsString("breeze")))))
    status(result) mustBe CREATED
  }

  "Delete a Non User" in {
    val Some(result) = route(app, FakeRequest(DELETE, "/users/nonExistentUser"))
    status(result) mustBe NOT_FOUND
  }

  "Delete an User" in {
    val query = BSONDocument()
    val Some(user) =  await(users.flatMap(_.find(query).one[User]))
    val Some(result) = route(app, FakeRequest(DELETE, "/users/" + user.username))
    status(result) mustBe OK
  }

  "Update an User" in {
    val payload = JsObject(List("_id"->JsString("id"), "username"->JsString("updated username"), "password"->JsString("updated password")))
    val query = BSONDocument()
    val Some(user) =  await(users.flatMap(_.find(query).one[User]))
    val Some(result) = route(app, FakeRequest(PATCH, "/users/" + user.username).withJsonBody(payload))

    status(result) mustBe OK
  }

  "Create a Token for the user" in {
    val query = BSONDocument()
    val Some(user) =  await(users.flatMap(_.find(query).one[User]))
    val Some(result) = route(app, FakeRequest(POST, "/users/" + user.username + "/token"))
    val token = contentAsJson(result).as[Token]
    status(result) mustBe CREATED
    token.username mustEqual user.username
    token.token.length > 0 mustBe true
  }

  after {
    users.flatMap(_.drop(failIfNotFound = false))
  }
}
