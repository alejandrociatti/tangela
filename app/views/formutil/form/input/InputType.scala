package views.formutil.form.input

/**
  * Created by Javier Isoldi.
  * Date: 09/09/14.
  * Project: edmin-template.
  */

object InputType extends Enumeration {
   type InputType = Value
   val Email = Value("email")
   val Text = Value("text")
   val Number = Value("number")
   val Password = Value("password")
   val Date = Value("date")
   val File = Value("file")
 }
