package views.formutil.form

import views.formutil.form.validations.Constraints._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Forms}
import play.api.data.Forms._

/**
 * Created by Javier Isoldi.
 * Date: 09/09/14.
 * Project: edmin-template.
 */
object Fields {


  val integer = number verifying required

  val long = longNumber verifying required

  val bigDecimal = Forms.bigDecimal verifying required

  val date = Forms.date verifying required
}
