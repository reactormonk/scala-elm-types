package elmtype

import ElmType._
import ElmTypeShapeless._
import utest._
import elmtype.derive._

object BasicTest extends TestSuite {
  case class Basic(a: Int, b: String)

  val tests = TestSuite {
    'basic - {
      val elmtype = MkElmType[Basic].elm
      elmtype match {
        case p: ProductType[Basic] =>
          assert(p.typeable.describe == "Basic")
        case _ => throw new Exception("Wrong elm type.")
      }
    }
  }
}
