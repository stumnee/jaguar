package models

import javax.inject.Inject

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}
import play.api.libs.json.Json.JsValueWrapper
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

case class User (
  name: String,
  email: String,
  password: String

)

class UserRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {
  import JsonFormats._


  def eventsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("users"))



  def getAll(): Future[Seq[User]] = {



    eventsCollection.flatMap(_.find()
      .cursor[User](ReadPreference.primary)
      .collect[Seq](100, Cursor.FailOnError[Seq[User]]()))
  }

}