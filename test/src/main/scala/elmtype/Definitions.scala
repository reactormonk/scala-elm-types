package elmtype
import java.time.Instant

case class Basic(a: Int, b: String)

sealed trait Sealed
case class S(s: String) extends Sealed
case class I(i: Int) extends Sealed

case class Listy(a: List[Int], b: Option[String])

case class Datey(a: Instant)

case class Longy(l: Long)

case class Nested(a: Int, b: Basic)

case class Identifier[T[_]](id: Long, name: String)

case class Character[T](t: T)
