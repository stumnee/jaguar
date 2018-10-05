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
  var token1: Token = _
  var token2: Token = _
  var token3: Token = _

  before {
    await {
      users = reactiveMongoApi.database.map(_.collection("users"))

      username = "Jane"

      users.flatMap(_.insert[User](ordered = false).many(List(
        User(_id = None, username = username, password = "badpass")
      )))


    }
    await {
      token1 = Token(_id=BSONObjectID.generate(), username=username, token=TokenRepository.generateToken(), revoked =  None, expiry = new DateTime())
      token2 = Token(_id=BSONObjectID.generate(), username=username, token=TokenRepository.generateToken(), revoked =  None, expiry = new DateTime())
      token3 = Token(_id=BSONObjectID.generate(), username=username, token=TokenRepository.generateToken(), revoked =  None, expiry = new DateTime())
      tokens = reactiveMongoApi.database.map(_.collection("tokens"))
      tokens.flatMap(_.insert[Token](ordered = false).many(List(
        token1,
        token2,
        token3
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
    val Some(tokenListResults) = route(app, FakeRequest(GET, s"/users/$username/token"))
    status(tokenListResults) mustBe OK

    val allTokens = contentAsJson(tokenListResults).as[List[Token]]

    allTokens.nonEmpty mustBe true
    allTokens.count(_.token == token1.token) mustBe 1
    allTokens.count(_.token == token2.token) mustBe 1
  }

  "Revoke a token" in {
    val Some(result) = route(app, FakeRequest(PATCH, s"/users/$username/token/${token1.token}?action=revoke"))
    status(result) mustBe OK


    val q = BSONDocument("_id" -> token1._id)
    val Some(t) = await(tokens.flatMap(_.find(q).one[Token]))

    t.revoked.isEmpty mustBe false
  }

  "Unrevoke a token" in {
    val Some(result) = route(app, FakeRequest(PATCH, s"/users/$username/token/${token1.token}?action=revoke"))
    status(result) mustBe OK


    val q = BSONDocument("_id" -> token1._id)
    val Some(tokenRevoked) = await(tokens.flatMap(_.find(q).one[Token]))

    tokenRevoked.revoked.isEmpty mustBe false

    val Some(unrevokeResult) = route(app, FakeRequest(PATCH, s"/users/$username/token/${token1.token}?action=unrevoke"))
    status(unrevokeResult) mustBe OK

    val Some(tokenUnrevoked) = await(tokens.flatMap(_.find(q).one[Token]))

    tokenUnrevoked.revoked.isEmpty mustBe true
  }

  "Invalid token action with no action param specified" in {
    val Some(result) = route(app, FakeRequest(PATCH, s"/users/$username/token/${token1.token}"))
    status(result) mustBe BAD_REQUEST
  }

  after {
    tokens.flatMap(_.drop(failIfNotFound = false))
    users.flatMap(_.drop(failIfNotFound = false))
  }
}
