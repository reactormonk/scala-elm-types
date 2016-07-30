package elmtype

import utest._
import elmtype._
import elmtype.derive._
import ElmTypeShapeless._

case class Basic(a: Int, b: String)

sealed trait Sealed
case class S(s: String) extends Sealed
case class I(i: Int) extends Sealed

object BasicTest extends TestSuite {

  val tests = TestSuite {
    'basic - {
      val elmtype = MkElmType[Basic].elm
      elmtype match {
        case p: ProductType[Basic] =>
          assert(p.typeable.describe == "Basic")
        case _ => throw new Exception("Wrong elm type.")
      }
      assert(AST.typeAST(elmtype).render == "type alias Basic = { a : Int, b : String }")
    }
    'sum - {
      val elmtype = MkElmType[Sealed].elm
      elmtype match {
        case s: SumType[Sealed] =>
          assert(s.typeable.describe == "Sealed")
        case _ => throw new Exception("Wrong elm type.")
      }

      val result =
    """type Sealed = SealedI I | SealedS S
      |type alias I = { i : Int }
      |type alias S = { s : String }""".stripMargin

      assert(AST.typeAST(elmtype).render == result)
    }
  }
}
