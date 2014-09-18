package util

import java.sql.Date
import org.joda.time.DateTime
import scala.slick.lifted.MappedTypeMapper
import slick.lifted.TypeMapper.DateTypeMapper

/**
  * Created by Javier Isoldi.
 * Date: 18/09/14.
 * Project: tangela.
 */

object DateTimeMapper {
  implicit def dateTime  = MappedTypeMapper.base[DateTime, Date] (
    dateTime => new Date(dateTime.getMillis),
    date => new DateTime(date)
  )

}

