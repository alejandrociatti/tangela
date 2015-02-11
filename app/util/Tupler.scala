package util

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 10/02/15
 * Time: 15:16
 *
 * This object receives request string 'tuples' and returns actual Tuple2 tuples.
 */
object Tupler {

  def toTuple(request:String):(String, String)= request match {
    case "" => ("", "")
    case string =>
      val split = string.split(",")
      val one = split.head.filter(!"(".contains(_))
      val two = split.tail.head.filter(!")".contains(_))
      (one, two)

  }

  def toQualityTuple(request:String):(Int, Int) = toTuple(request) match {
    case ("", "") => (-1, -1)
    case (one, "") => (Integer.parseInt(one), -1)
    case ("", two) => (-1, Integer.parseInt(two))
    case tuple => (Integer.parseInt(tuple._1), Integer.parseInt(tuple._2))
  }

}
