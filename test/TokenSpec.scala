import models.{Token, TokenRepository, User}
import org.scalatest.BeforeAndAfter
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, await, contentAsJson, route, status}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.UserJsonFormats._
import models.TokenJsonFormats._
import org.joda.time.DateTime
import reactivemongo.play.json._
import play.api.test.Helpers._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TokenSpec extends PlayWithMongoSpec with BeforeAndAfter {
  var users: Future[JSONCollection] = _
  var username: String = _
  var tokens: Future[JSONCollection] = _

  before {
    await {
      users = reactiveMongoApi.database.map(_.collection("users"))

      username = "Jane"

      users.flatMap(_.insert[User](ordered = false).many(List(
        User(_id = None, username = username, password = "badpass")
      )))


    }
    await {
      tokens = reactiveMongoApi.database.map(_.collection("tokens"))
      tokens.flatMap(_.insert[Token](ordered = false).many(List(
        Token(_id=BSONObjectID.generate(), username=username, token=TokenRepository.generateToken(), revoked =  None, expiry = new DateTime()),
        Token(_id=BSONObjectID.generate(), username=username, token=TokenRepository.generateToken(), revoked =  None, expiry = new DateTime())
      )))
    }

  }

  "Create a Token for the user" in {
    val Some(result) = route(app, FakeRequest(POST, s"/users/$username/token"))
    val token = contentAsJson(result).as[Token]
    status(result) mustBe CREATED

    token.username mustEqual username
    token.token.length > 0 mustBe true
  }

  "List all tokens of a non-existent user" in {
    val Some(tokenListResults) = route(app, FakeRequest(GET, "/users/nonExistentUser/token"))
    status(tokenListResults) mustBe NOT_FOUND
  }

  "List all tokens" in {
    val Some(tokenResult) = route(app, FakeRequest(POST, s"/users/$username/token"))
    val token = contentAsJson(tokenResult).as[Token]
    status(tokenResult) mustBe CREATED
    val Some(tokenListResults) = route(app, FakeRequest(GET, s"/users/$username/token"))
    status(tokenListResults) mustBe OK
    val allTokens = contentAsJson(tokenListResults).as[List[Token]]
    allTokens.nonEmpty mustBe true
    allTokens.count(_.token == token.token) mustBe 1
  }

  "Revoke a token" in {
    val query = BSONDocument()
    val Some(token) = await(tokens.flatMap(_.find(query).one[Token]))

    val Some(result) = route(app, FakeRequest(PATCH, s"/users/$username/token?action=revoke"))
    status(result) mustBe OK
  }

  after {
    tokens.flatMap(_.drop(failIfNotFound = false))
    users.flatMap(_.drop(failIfNotFound = false))
  }
}
