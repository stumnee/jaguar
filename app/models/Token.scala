package models

import java.math.BigInteger
import java.security.SecureRandom
import javax.inject.Inject

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.Results
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

case class Token (
  _id: BSONObjectID,
  userId: BSONObjectID,
  token: String,
  revoked: Option[DateTime],
  expiry: DateTime
)

object TokenJsonFormats{
  import play.api.libs.json._
  import play.api.libs.json.JodaWrites
  import play.api.libs.json.JodaReads

  val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val dateFormat = Format[DateTime](JodaReads.jodaDateReads(pattern), JodaWrites.jodaDateWrites(pattern))

  implicit val tokenFormat: OFormat[Token] = Json.format[Token]

}

class TokenRepository @Inject()(implicit  ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {
  val DefaultExpirationDays = 7
  val TokenSize = 64

  import TokenJsonFormats._

  def tokensCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("tokens"))

  def generateToken() : String = {
    val random = new SecureRandom()
    new BigInteger(TokenSize * 5, random).toString(32)
  }

  def create(userId: BSONObjectID): Future[Option[Token]] = {
    val token = Token(BSONObjectID.generate(), userId, generateToken(), None, new DateTime().plusDays(DefaultExpirationDays))

    for {
      _ <- tokensCollection.flatMap(_.insert(token))
      createdToken <- tokensCollection.flatMap(_.find(BSONDocument("_id" -> token._id)).one[Token])
    } yield {
      createdToken
    }

  }

  def getToken(tokenStr: String): Future[Option[Token]] = {
    val query = Json.obj("token"->tokenStr)
    tokensCollection.flatMap(_.find(query).one[Token])
  }
}