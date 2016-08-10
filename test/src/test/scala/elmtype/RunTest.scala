package elmtype

import utest._
import elmtype._
import elmtype.derive._
import ElmTypeShapeless._
import language.implicitConversions
import shapeless._
import scalaz._

object Main extends ElmTypeMain(ToElmTypes[Basic :: Sealed :: HNil].apply)
