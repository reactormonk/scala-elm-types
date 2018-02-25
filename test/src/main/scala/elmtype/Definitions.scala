package elmtype
import java.time.Instant

case class Basic(a: Int, where: String)

sealed trait Sealed
case class S(s: String) extends Sealed
case class I(i: Int) extends Sealed

case class Listy(a: List[Int], b: Option[String])

case class Datey(a: Instant)

case class Longy(l: Long)

case class Nested(a: Int, b: Basic)

case class Identifier[T[_]](id: Int, name: String)

case class Character[T](t: T)

object Aliases {
  type CompressedCharacter = Identifier[Character]
}

case class Alias(t: Aliases.CompressedCharacter)

sealed trait NestedAST
case class NestedI(i: Int) extends NestedAST
sealed trait Nested2 extends NestedAST
case class NestedS(s: String) extends Nested2
case class NestedF(f: Float) extends Nested2

sealed trait CaseObjectAST
case class CaseClass(i: Int) extends CaseObjectAST
case object CaseObject extends CaseObjectAST

case class NestedTypes(x: Option[List[String]])
