package views.formutil.common

import play.api.templates.Html

/**
 * Created by Javier Isoldi.
 * Date: 09/09/14.
 * Project: edmin-template.
 */
object HtmlUtil {

  val PropertyPrefix = "_"

  def attributesToHtml(arguments: Seq[(Symbol, Any)]) = Html(arguments.map(attributeToHtml) mkString " ")

  def attributeToHtml(attribute: (Symbol, Any)): String =  {
    if (attribute._1.name.startsWith("_"))
      attribute match {
        case (symbol, value: Boolean) => if (value) symbol.name + "=\"" + symbol.name + "\"" else ""
        case (symbol, value) => symbol.name + "=" + "\"" + value + "\""
      }
    else ""
  }
}
