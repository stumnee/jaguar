package models

import java.math.BigInteger
import java.security.SecureRandom
import javax.inject.Inject

import models.UserJsonFormats.encryptPassword
import org.joda.time.DateTime
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

case class Token (
  userId: BSONObjectID,
  token: String,
  expiry: DateTime
)

class TokenRepository @Inject()(implicit  ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {
  val defaultExpirationDays = 7

  def tokensCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("tokens"))

  def generateToken() : String = {
    val random = new SecureRandom()
    new BigInteger(24 * 5, random).toString(32)
  }

  def create(userId: BSONObjectID): Future[WriteResult] = {
    val token: Token = Token(userId, generateToken(), new DateTime().plusDays(defaultExpirationDays))

    tokensCollection.flatMap(_.insert(token))
  }
}