package models

import sorm._
import models.authentication._

/**
 * Created by Javier Isoldi.
 * Date: 17/09/14.
 * Project: tangela.
 */

/**
 * Database configuration.
 * This object contains the entities and the database configuration.
 * */

object Database extends Instance(
  entities =
    Set(User.getEntity, Role.getEntity) ++
      Set(Location.getEntity, StartupLocation.getEntity) ++
      Set(Market.getEntity, StartupMarket.getEntity) ++
      Set(Startup.getEntity),
  url = "jdbc:h2:file:tangelaDB/tangelaDB",
  user = "sa",
  password = "",
  initMode = InitMode.DropCreate,
  poolSize = 12
)
