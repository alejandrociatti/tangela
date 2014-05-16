package controllers

import play.api.data.Forms._
import anorm.{Pk, NotAssigned}
import play.api.data.Form
import models.Startup

/**
 * Created by Javi on 5/16/14.
 */
object Startups {

  val startupForm = Form(
    mapping(
      "id" -> ignored(NotAssigned:Pk[Long]),
      "name" -> nonEmptyText
    )(Startup.apply)(Startup.unapply)
  )
}
