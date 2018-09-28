package utils

import org.joda.time.DateTime
import play.api.libs.json._

object JsonFormats {
  val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val dateFormat = Format[DateTime](
    (__ \ "$date").read[Long].map { date =>
      new DateTime(date)
    }, new Writes[DateTime] {
      override def writes(o: DateTime): JsValue = Json.obj("$date" -> o.getMillis())
    })
}
class JsonFormats {

}
