package models

import javax.inject.Inject

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.functional.syntax._

import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import play.api.libs.json.Json.JsValueWrapper

import scala.util.parsing.json.JSONObject


case class EventDao (
   tags: Seq[String],
   files: Option[Seq[String]],
   title: String,
   data: String
)

case class Event (
  id:  Option[String],
  tags: Seq[String],
  files: Seq[String],
  title: String,
  data: String,
  createdTime: Option[DateTime]
)

object EventJsonFormats{
  import play.api.libs.json._
  import play.api.libs.json.JodaWrites
  import play.api.libs.json.JodaReads

  implicit object eventFormat extends OFormat[Event] {
    override def reads(json: JsValue): JsResult[Event] = {
      val jsonObject = Json.parse(json.toString())

      var id: BSONObjectID = null
      var files: Seq[String] = null

      (jsonObject \ "_id").validate[BSONObjectID] match {
        case JsSuccess(value, _) => id = value
        case error: JsError => id = BSONObjectID.generate()
      }
      (jsonObject \ "files").validate[Seq[String]] match {
        case JsSuccess(value, _) => files = value
        case error: JsError => files = Seq()
      }

      JsSuccess(Event(
        Some(id.stringify),
        (jsonObject \ "tags").as[Seq[String]],
        files,
        (jsonObject \ "title").as[String],
        (jsonObject \ "data").as[String],
        Some(new DateTime(id.time))))

    }

    override def writes(event: Event): JsObject = {
      JsObject(Seq(
        "id" -> JsString(event.id.get),
        "tags" -> JsArray(event.tags.map{ tag=>
          JsString(tag)
        }),
        "files" -> JsArray(event.tags.map{ file=>
          JsString(file)
        }),
        "title" -> JsString(event.title),
        "data" -> JsString(event.data),
        "created" -> JsString(event.createdTime.get.toString)
      ))
    }
  }

  val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val dateFormat = Format[DateTime](JodaReads.jodaDateReads(pattern), JodaWrites.jodaDateWrites(pattern))


//  implicit val idFormat: OFormat[BSONObjectID] = Json.format[BSONObjectID]

//  def idFormat[T]: Format[BSONObjectID] = Format(
//    __.read[Long].map(BSONObjectID(_)),
//    new Writes[BSONObjectID]{ def writes(o: BSONObjectID) = JsNumber(o.id) }
//  )

  implicit val eventDaoFormat: OFormat[EventDao] = Json.format[EventDao]
//
//  implicit val eventFormat: OFormat[Event] = (
//    (__ \ "id").formatNullable[BSONObjectID] and
//    (__ \ "title").format[String] and
//      (__ \ "data").format[String] and
//      (__ \ "created").formatNullable[DateTime]
//  )(Event.apply, unlift(Event.unapply))

//  implicit val eventReads: Reads[Event] = (
//    (JsPath \ "id").read[BSONObjectID] and
//      (JsPath \ "title").read[String] and
//      (JsPath \ "data").read[String] and
//      (JsPath \ "created").read[DateTime]
//    )(Event.apply _)
//
//  implicit val eventWrites: Writes[Event] = (
//    (JsPath \ "id").write[Option[BSONObjectID]] and
//    (JsPath \ "title").write[String] and
//      (JsPath \ "data").write[String] and
//      (JsPath \ "created").write[Option[DateTime]]
//  )(unlift(Event.unapply))


}

class EventRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {
  import EventJsonFormats._


  def eventsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("events"))



  def getAll(queryString: Map[String,Seq[String]]): Future[Seq[Event]] = {
//    val query = Json.obj("title"->Json.obj("$lt"->BSONObjectID("5b3dde17c82b9ea9311bb451")))
    val query = getQuery(queryString)


    val cc = queryString.get("_count")
    val count: Int = cc match {
      case Some(Vector(value)) => value.toInt
      case _ => 100
    }

    val sortOptions: Option[JsObject] = getSort(queryString.get("_sort"), Vector("id", "title", "data"))

    eventsCollection.flatMap(_.find(query)
        .sort(sortOptions.getOrElse(Json.obj()))
        .options(QueryOpts(skipN = getSkipCount(count, queryString)))
      .cursor[Event](ReadPreference.primary)
      .collect[Seq](count, Cursor.FailOnError[Seq[Event]]()))
  }

  def get(id: BSONObjectID): Future[Option[Event]] = {
    val query = BSONDocument("_id" -> id)
    eventsCollection.flatMap(_.find(query).one[Event].map{item=>
      item
    })
  }

  def add(item: EventDao): Future[WriteResult] = {
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

  private def getSort(sortFields: Option[Seq[String]], sortableFields: Vector[String]): Option[JsObject] =
    sortFields.map { fields =>
      val sortBy = for {
        order <- fields.map { field =>
          if (field.startsWith("-"))
            field.drop(1) -> -1
          else field -> 1
        }
        if sortableFields.contains(order._1)// == "title" || order._1 == "_id"
      } yield order._1.replace("id", "_id") -> implicitly[Json.JsValueWrapper](Json.toJson(order._2))

      Json.obj(sortBy: _*)
    }

  private def getFieldValue(field: String, value: String): JsValueWrapper = {
    if (field == "id") {
      BSONObjectID(value)
    } else if (field == "created") {
      BSONObjectID.fromTime(DateTime.parse(value).getMillis)
    } else {
      value
    }
  }
  private def getFieldName(field: String): String = {
    if (field == "id" || field == "created") {
      "_id"
    } else {
      field
    }
  }

  def getSkipCount(count: Int, queryString: Map[String, Seq[String]]): Int = {
    var skip = 0
    queryString.get("_offset") match {
      case Some(n) => skip = n.head.toInt
      case _ =>
    }

    val page = queryString.get("_page") match {
      case Some(v) => v.head.toInt - 1
      case _ => 0
    }

    skip + count * page
  }

  private def getQuery(queryFields: Map[String, Seq[String]]): JsObject = {
    val filterBy = queryFields
        .filter(!_._1.startsWith("_"))
        .map { case (field, values) =>
          Map(">"->"$gt", "<"->"$lt").filterKeys(field.contains(_)).map { t =>
            val tokens = field.split(t._1)
            getFieldName(tokens.head) -> implicitly[Json.JsValueWrapper](Json.obj(t._2 -> getFieldValue(tokens.head, tokens(1))))
          }.headOption match {
            case Some(obj)  => obj
            case _          => getFieldName(field) -> implicitly[Json.JsValueWrapper](getFieldValue(field, values.head))
          }

        }.toSeq
    Json.obj(filterBy: _*)
  }
}

