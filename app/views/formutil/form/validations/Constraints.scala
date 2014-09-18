package views.formutil.form.validations

import play.api.data.Forms.{longNumber, email, number}
import play.api.data.{Forms, FormError}
import play.api.data.validation._
import play.api.templates.Html

import scala.collection.mutable

/**
 * Created by Javier Isoldi.
 * Date: 20/07/14.
 * Project: edmin-template.
 */
object Constraints {

  def constraintsToHtml(constraints: Seq[(String, Seq[Any])]) = Html {
    constraints.foldLeft("")((value, constraint) => value + " " + constraintToHtml(constraint))
  }

  def constraintToHtml(constraint: (String, Any)): String = constraint match {
    case ("constraint.required",_) => "required=\"required\""
    case ("constraint.min", array: mutable.WrappedArray[Any] ) => "min=\"" + array.head + "\""
    case ("constraint.max", array: mutable.WrappedArray[Any] ) => "max=\"" + array.head + "\""
    case ("constraint.minLength", array: mutable.WrappedArray[Any] ) =>
      "pattern=.{" + array.head +",} title=\"Value must be " + array.head + " characters minimum\""
    case ("constraint.maxLength", array: mutable.WrappedArray[Any] ) => "maxLength=\"" + array.head + "\""
    case _ => ""
  }

  def required[T]: Constraint[T] = Constraint[T]("constraint.required") { o =>
    if (o == None) Invalid(ValidationError("error.required")) else Valid
  }

}

