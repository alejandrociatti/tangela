package models

import anorm.{NotAssigned, Pk}

/**
 * Created by Javi on 5/15/14.
 */
case class Startup(id: Pk[Long] = NotAssigned, name: String, angelId:Long)


