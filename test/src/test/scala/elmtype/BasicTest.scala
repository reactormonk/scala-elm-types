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
      val ast = AST.typeAST(elmtype)
      assert(AST.render(ast) == """import Date
type alias Basic = { a : Int, b : String }""")
      assert(AST.decoder(ast) ==
        """import Json.Decode.Extra
import Json.Decode exposing (..)
decodeBasic : Decoder Basic
decodeBasic =
  succeed Basic |: ("a" := int) |: ("b" := string)""")
      assert(AST.encoder(ast) == """import Json.Encode exposing (..)
import Date.Extra exposing (toUtcIsoString)
encodeBasic : Basic -> Value
encodeBasic = object
  [ ("a", int obj.a)
  , ("b", string obj.b)
  ]""")
    }

    'sum - {
      val elmtype = MkElmType[Sealed].elm
      elmtype match {
        case s: SumType[Sealed] =>
          assert(s.typeable.describe == "Sealed")
        case _ => throw new Exception("Wrong elm type.")
      }

      val typeDecl =
        """import Date
type Sealed = SealedI I | SealedS S
type alias I = { i : Int }
type alias S = { s : String }"""

      val decodeDecl =
        """import Json.Decode.Extra
import Json.Decode exposing (..)
decodeSealed : Decoder Sealed
decodeSealed = oneOf
  [ ("I" := decodeI)
  , ("S" := decodeS)
  ]
decodeI : Decoder I
decodeI =
  succeed I |: ("i" := int)
decodeS : Decoder S
decodeS =
  succeed S |: ("s" := string)"""

      val encodeDecl =
        """import Json.Encode exposing (..)
import Date.Extra exposing (toUtcIsoString)

encodeSealed: Sealed -> Value
encodeSealed obj =
  let
    (typefield, inner) = case obj of
      I obj2 -> ("I", encode{name} obj2)
      S obj2 -> ("S", encode{name} obj2)
    in
      object [(typefield, (object inner))]
encodeI : I -> Value
encodeI = object
  [ ("i", int obj.i)
  ]
encodeS : S -> Value
encodeS = object
  [ ("s", string obj.s)
  ]"""

      val ast = AST.typeAST(elmtype)
      assert(AST.render(ast) == typeDecl)
      assert(AST.decoder(ast) == decodeDecl)
      assert(AST.encoder(ast) == encodeDecl)
    }
  }
}
