package util

import play.api.libs.json.{Json, JsArray, JsObject}

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 17/02/15
 * Time: 00:58
 */
object JsArrayer {

  def toJsArray(objects:Seq[JsObject]) = objects.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))

}
