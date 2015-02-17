package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 20:57
 */
case class AngelUser(id: Long, name:String, bio:Option[String], roles:Option[Seq[AngelRole]], followerCount:Option[Int],
                      angelURL:Option[String], blogURL:Option[String], bioURL:Option[String], twitterURL:Option[String],
                      facebookURL:Option[String], linkedInURL:Option[String], investor:Option[Boolean]){

  def toTinyJson = Json.obj("id" -> id, "name" -> name)
}

object AngelUser{

  implicit val userReads : Reads[AngelUser] = (
      (__ \ "id").read[Long] and
      (__ \ "name").read[String] and
      (__ \ "bio").readNullable[String] and
      (__ \ "roles").readNullable[Seq[AngelRole]] and
      (__ \ "follower_count").readNullable[Int] and
      (__ \ "angellist_url").readNullable[String] and
      (__ \ "blog_url").readNullable[String] and
      (__ \ "online_bio_url").readNullable[String] and
      (__ \ "twitter_url").readNullable[String] and
      (__ \ "facebook_url").readNullable[String] and
      (__ \ "linkedin_url").readNullable[String] and
      (__ \ "investor").readNullable[Boolean]
    )(AngelUser.apply _)

  implicit val userWrites :Writes[AngelUser] = (
      (__ \ "id").write[Long] and
      (__ \ "name").write[String] and
      (__ \ "bio").writeNullable[String] and
      (__ \ "roles").writeNullable[Seq[AngelRole]] and
      (__ \ "follower_count").writeNullable[Int] and
      (__ \ "angellist_url").writeNullable[String] and
      (__ \ "blog_url").writeNullable[String] and
      (__ \ "online_bio_url").writeNullable[String] and
      (__ \ "twitter_url").writeNullable[String] and
      (__ \ "facebook_url").writeNullable[String] and
      (__ \ "linkedin_url").writeNullable[String] and
      (__ \ "investor").writeNullable[Boolean]
    )(unlift(AngelUser.unapply))

}