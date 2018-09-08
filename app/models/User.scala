package models

import javax.inject.Inject

import org.mindrot.jbcrypt.BCrypt
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.Json
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

case class UserDao (
  username: String,
  password: String
)
case class User (
  id:  Option[String],
  username: String,
  password: String
)


object UserJsonFormats{
  import play.api.libs.json._

  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val userDaoFormat: OFormat[UserDao] = Json.format[UserDao]
}

class UserRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  import UserJsonFormats._

  def usersCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("users"))



  def add(item: UserDao): Future[WriteResult] = {
    val user: User = User(None, item.username, BCrypt.hashpw(item.password, BCrypt.gensalt()))
    usersCollection.flatMap(_.insert(user))
  }


  def delete(username: String): Future[Option[User]] = {
    val selector = BSONDocument("username" -> username)
    usersCollection.flatMap(_.findAndRemove(selector).map(_.result[User]))
  }

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