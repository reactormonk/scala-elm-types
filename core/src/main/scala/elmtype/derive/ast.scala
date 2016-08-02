package elmtype

import shapeless._

sealed trait ElmTypeAST {
  def dependent: List[ElmNamedType]
  def name: String
}
sealed trait ElmStandaloneType extends ElmTypeAST {
  def name: String
  def decoderName: String
  def encoderName: String
}
sealed trait ElmNamedType extends ElmStandaloneType {
  def render: String
  // def encoder: String
  def decoder: String
  def decoderName = s"decode${name}"
  def encoder: String
  def encoderName = s"encode{name}"
}

case class ASTAlias(name: String, fields: List[ASTField], innerDependent: List[ElmNamedType]) extends ElmNamedType {
  val dependent = this :: innerDependent
  val renderedFields = fields.map({ f => s"${f.name} : ${f.typeDecl}"})
  val render = s"type alias $name = { ${renderedFields.mkString(", ")} }"
  def decoder = {
    val types = fields.map({ f => s"""("${f.name}" := ${f.inner.decoderName})"""})
    s"""decode${name} : Decoder ${name}
decode${name} =
  succeed ${name} |: ${types.mkString(" |: ")}"""
  }
  def encoder = {
    val types = fields.map({ f => s"""("${f.name}", ${f.inner.encoderName} obj.${f.name})"""})
    s"""encode${name} : ${name} -> Value
encode${name} = object
  [ ${types.mkString("\n  , ")}
  ]"""
  }
}
case class ASTSum(name: String, fields: List[ASTField], innerDependent: List[ElmNamedType], typefield: Option[String]) extends ElmNamedType {
  val dependent = this :: innerDependent
  val renderedFields = fields.map({ f => s"${name}${f.name} ${f.typeDecl}"})
  val render = s"type $name = ${renderedFields.mkString(" | ")}"
  def decoder = typefield match {
    case Some(field) => {
      val types = fields.map({ f =>
        s""""  ${f.name}" ->
    ${f.inner.decoderName}"""
      })
      s"""decode${name} : Decoder $name
decode${name} = ("${field}" := string) `andThen` (\typefield ->
${types.mkString("\n")}
  _ -> fail (typefield ++ " is not recognized among ${fields.map(_.name).mkString(" ")}")
  )"""
    }
    case None => {
      val types = fields.map({ f => s"""("${f.name}" := decode${f.name})"""})
      s"""decode${name} : Decoder $name
decode${name} = oneOf
  [ ${types.mkString("\n  , ")}
  ]"""
    }
  }
  def encoder = {
    val outer = typefield match {
      case Some(field) => s"""object (inner ++ [("${field}", string typefield )])"""
      case None => s"""object [(typefield, (object inner))]"""
    }
    val types = fields.map({ f => s"""${f.name} obj2 -> ("${f.name}", ${f.inner.encoderName} obj2)"""})
    s"""
encode${name}: ${name} -> Value
encode${name} obj =
  let
    (typefield, inner) = case obj of
      ${types.mkString("\n      ")}
    in
      ${outer}"""
  }
}
case class ASTField(name: String, inner: ElmStandaloneType) extends ElmTypeAST {
  val typeDecl = inner.name
  val dependent = inner.dependent
}
case class RawAST(typeDecl: String, dependent: List[ElmNamedType], decoderName: String, encoderName: String) extends ElmStandaloneType {
  val name = typeDecl
}

object AST {
    def typeAST(s: SumType[_]): ASTSum = {
    val inner = s.types.map(typeAST)
    val dependents = inner.flatMap(_.dependent)
    ASTSum(s.typeable.describe, inner, dependents, s.typefield)
  }
  def typeAST(p: ProductType[_]): ASTAlias = {
    val inner = p.types.map(typeAST)
    val dependents = inner.flatMap(_.dependent)
    ASTAlias(p.typeable.describe, inner, dependents)
  }
  def typeAST(e: ElmField[_]): ASTField = {
    val inner = typeAST(e.innerType)
    ASTField(e.name, inner)
  }
  def typeAST(r: RawType[_]): RawAST = {
    RawAST(r.name, List(), r.decoder, r.encoder)
  }
  def typeAST[T[_]](h: HigherType[T, _]): RawAST = {
    val inner = typeAST(h.innerType)
    RawAST(s"${h.name} ${inner.name}", inner.dependent, s"${h.decoder} ${inner.decoderName}", s"${h.encoder} ${inner.encoderName}}")
  }

  def typeAST(h: ValidSubType[_]): ElmStandaloneType = {
    h match {
      case t: SumType[_] => typeAST(t)
      case t: ProductType[_] => typeAST(t)
      case t: RawType[_] => typeAST(t)
      case t: HigherType[_, _] => typeAST(t)
    }
  }

  def typeAST(h: CombinedType[_]): ElmNamedType = {
    h match {
      case t: SumType[_] => typeAST(t)
      case t: ProductType[_] => typeAST(t)
    }
  }

  def typeAST(h: ElmType[_]): ElmTypeAST = {
    h match {
      case t: SumType[_] => typeAST(t)
      case t: ProductType[_] => typeAST(t)
      case t: ElmField[_] => typeAST(t)
      case t: RawType[_] => typeAST(t)
      case t: HigherType[_, _] => typeAST(t)
    }
  }

  def render(t: ElmNamedType): String = {
    """import Date
""" +
    t.dependent.distinct.map(_.render).mkString("\n")
  }
  // http://package.elm-lang.org/packages/elm-community/json-extra/1.0.0/Json-Decode-Extra
  def decoder(t: ElmNamedType): String = {
    """import Json.Decode.Extra
import Json.Decode exposing (..)
""" +
    t.dependent.distinct.map(_.decoder).mkString("\n")
  }

  // http://package.elm-lang.org/packages/justinmimbs/elm-date-extra/2.0.0/
  def encoder(t: ElmNamedType): String = {
    """import Json.Encode exposing (..)
import Date.Extra exposing (toUtcIsoString)
""" +
    t.dependent.distinct.map(_.encoder).mkString("\n")
  }

}
