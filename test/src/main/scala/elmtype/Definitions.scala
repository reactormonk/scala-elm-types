package elmtype
import java.time.Instant

case class Basic(a: Int, b: String)

sealed trait Sealed
case class S(s: String) extends Sealed
case class I(i: Int) extends Sealed

case class Listy(a: List[Int], b: Option[String])

case class Datey(a: Instant)

case class Longy(l: Long)
