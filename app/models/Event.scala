package models

import javax.inject.Inject

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

case class Event (
  _id:  Option[BSONObjectID],
  title: String,
  data: String
)

object JsonFormats{
  import play.api.libs.json._

  implicit val eventFormat: OFormat[Event] = Json.format[Event]
}

class EventRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {
  import JsonFormats._

  def eventsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("events"))

  def getAll: Future[Seq[Event]] = {
    val query = Json.obj()
    eventsCollection.flatMap(_.find(query)
      .cursor[Event](ReadPreference.primary)
      .collect[Seq](100, Cursor.FailOnError[Seq[Event]]())
    )
  }

  def get(id: BSONObjectID): Future[Option[Event]] = {
    val query = BSONDocument("_id" -> id)
    eventsCollection.flatMap(_.find(query).one[Event])
  }

  def add(item: Event): Future[WriteResult] = {
    eventsCollection.flatMap(_.insert(item))
  }

  def update(id: BSONObjectID, item: Event): Future[Option[Event]] = {

    val selector = BSONDocument("_id" -> id)
    val updateModifier = BSONDocument(
      "$set" -> BSONDocument(
        "title" -> item.title)
    )

    eventsCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true).map(_.result[Event])
    )
  }

  def delete(id: BSONObjectID): Future[Option[Event]] = {
    val selector = BSONDocument("_id" -> id)
    eventsCollection.flatMap(_.findAndRemove(selector).map(_.result[Event]))
  }
}