package models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 19/02/15
 * Time: 19:57
 */
case class Funding(id :Long, roundType:Option[String], amount:Long,
                   sourceUrl:Option[String], closedAt:Option[DateTime], participants:Option[Seq[Participant]]){

  def toCSVRows(startup:Startup) : Seq[Seq[String]] = {
    // this function makes a row for a single or empty participant
    def makeRow(participant: Option[Participant] = None): Seq[String] = Seq(
      DatabaseUpdate.getLastAsString,
      startup.id.toString, startup.name, roundType.getOrElse(""), amount.toString,
      closedAt.fold("")(_.toString), id.toString, sourceUrl.getOrElse(""),
      participant.fold("")(_.name), participant.fold("")(_.participantType), participant.fold("")(_.id.toString)
    )
    // if participants is empty, make an empty row, if not, make a row for each participant
    participants.fold( Seq(makeRow()) ){ participants =>
      participants.map(participant => makeRow(Option(participant)))
    }
  }
}

object Funding {

  implicit val fundingReads: Reads[Funding] = (
      (__ \ "id").read[Long] and
      (__ \ "round_type").readNullable[String] and
      (__ \ "amount").read[Long] and
      (__ \ "souce_url").readNullable[String] and
      (__ \ "closed_at").lazyReadNullable[DateTime](Startup.jodaTimeReads) and
      (__ \ "participants").readNullable[Seq[Participant]]
    )(Funding.apply _)

  implicit val fundingWrites: Writes[Funding] = (
      (__ \ "id").write[Long] and
      (__ \ "round_type").writeNullable[String] and
      (__ \ "amount").write[Long] and
      (__ \ "source_url").writeNullable[String] and
      (__ \ "closed_at").lazyWriteNullable[DateTime](Startup.jodaTimeWrites) and
      (__ \ "participants").writeNullable[Seq[Participant]]
    )(unlift(Funding.unapply))

  def getCSVHeader :Seq[String] = Seq(
    "tangela request date", "startup ID", "startup name", "round type", "round raised", "round closed at",
    "round id", "round source url", "participant name", "participant type", "participant id"
  )
}

case class Participant(id: Long, name: String, participantType:String)

private object Participant{

  implicit val participantReads : Reads[Participant] = (
      (__ \ "id").read[Long] and
      (__ \ "name").read[String] and
      (__ \ "type").read[String]
    )(Participant.apply _)

  implicit val participantWrites: Writes[Participant] = (
      (__ \ "id").write[Long] and
      (__ \ "name").write[String] and
      (__ \ "type").write[String]
    )(unlift(Participant.unapply))
}
