package models

import javax.inject.Inject

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.Json
import reactivemongo.play.json._
import scala.concurrent.{ExecutionContext, Future}

case class User (
  id:  Option[String],
  username: String,
  password: String
)


object UserJsonFormats{
  import play.api.libs.json._

  implicit val userFormat: OFormat[User] = Json.format[User]
}

class UserRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  import UserJsonFormats._

  def usersCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("users"))



  def getAll(): Future[Seq[User]] = {

    val query = Json.obj()

    usersCollection.flatMap(_.find(query)
      .cursor[User](ReadPreference.primary)
      .collect[Seq](100, Cursor.FailOnError[Seq[User]]()))
  }

  def getByUsername(username: String): Future[Option[User]] = {
    val query = BSONDocument("username" -> username)

    usersCollection.flatMap(_.find(query).one[User])
  }

}