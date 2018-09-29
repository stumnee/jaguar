import models.{Token, User}
import org.scalatest.BeforeAndAfter
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, await, contentAsJson, route, status}
import reactivemongo.bson.BSONDocument
import models.UserJsonFormats._
import models.TokenJsonFormats._
import reactivemongo.play.json._
import play.api.test.Helpers._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TokenSpec extends PlayWithMongoSpec with BeforeAndAfter {
  var users: Future[JSONCollection] = _
  var tokens: Future[JSONCollection] = _

  before {
    await {
      users = reactiveMongoApi.database.map(_.collection("users"))

      users.flatMap(_.insert[User](ordered = false).many(List(
        User(_id = None, username = "Jane", password = "badpass")
      )))

    }
    await {
      tokens = reactiveMongoApi.database.map(_.collection("tokens"))
      tokens.flatMap(_.insert[User](ordered = false).many(List(
      )))
    }

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

  "List all tokens of a non-existent user" in {
    val Some(tokenListResults) = route(app, FakeRequest(GET, "/users/nonExistentUser/token"))
    status(tokenListResults) mustBe NOT_FOUND
  }

  "List all tokens" in {
    val query = BSONDocument()
    val Some(user) =  await(users.flatMap(_.find(query).one[User]))
    val Some(tokenResult) = route(app, FakeRequest(POST, "/users/" + user.username + "/token"))
    val token = contentAsJson(tokenResult).as[Token]
    status(tokenResult) mustBe CREATED
    val Some(tokenListResults) = route(app, FakeRequest(GET, "/users/" + user.username + "/token"))
    status(tokenListResults) mustBe OK
    val allTokens = contentAsJson(tokenListResults).as[List[Token]]
    allTokens.nonEmpty mustBe true
    allTokens.count(_.token == token.token) mustBe 1
  }

  after {
    tokens.flatMap(_.drop(failIfNotFound = false))
    users.flatMap(_.drop(failIfNotFound = false))
  }
}
