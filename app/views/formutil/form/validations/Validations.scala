package views.formutil.form.validations

import play.api.data.FormError

import scala.collection.mutable

/**
 * Created by Javier Isoldi.
 * Date: 09/09/14.
 * Project: edmin-template.
 */
object Validations {

  def errorMessage(formError: FormError) = formError match {
    case FormError(_,"error.required",_) => "This field is required."
    case FormError(_,"error.min",array: mutable.WrappedArray[Any] ) =>
      "Value must be greater than or equals to " + array.head +"."
    case FormError(_,"error.max",array: mutable.WrappedArray[Any] ) =>
      "Value must be less than or equals to " + array.head +"."
    case FormError(_,"error.min.strict",array: mutable.WrappedArray[Any] ) =>
      "Value must be greater than " + array.head +"."
    case FormError(_,"error.max.strict",array: mutable.WrappedArray[Any] ) =>
      "Value must be less than " + array.head +"."
    case FormError(_,"error.minLength",array: mutable.WrappedArray[Any] ) =>
      "Value must be " + array.head + " characters minimum."
    case FormError(_,"error.maxLength",array: mutable.WrappedArray[Any] ) =>
      "Value must be " + array.head + " characters maximum."
    case FormError(_,"error.email", _) => "Value must be a valid email."
    case FormError(_,"error.number", _) => "Value must be a valid number."
    case _ => ""
  }
}
