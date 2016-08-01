package elmtype

import shapeless._

sealed trait ElmTypeAST {
  def dependent: List[ElmNamedType]
  def name: String
}
sealed trait ElmStandaloneType extends ElmTypeAST {
  def name: String
  def decoderName: String
}
sealed trait ElmNamedType extends ElmStandaloneType {
  def render: String
  // def encoder: String
  def decoder: String
  def decoderName = s"decode${name}"
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
}
case class ASTField(name: String, inner: ElmStandaloneType) extends ElmTypeAST {
  val typeDecl = inner.name
  val dependent = inner.dependent
}
case class RawAST(typeDecl: String, dependent: List[ElmNamedType], decoderName: String) extends ElmStandaloneType {
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
    RawAST(r.name, List(), r.decoder)
  }
  def typeAST[T[_]](h: HigherType[T, _]): RawAST = {
    val inner = typeAST(h.innerType)
    RawAST(s"${h.name} ${inner.name}", inner.dependent, s"${h.decoder} ${inner.decoderName}")
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
    t.dependent.distinct.map(_.render).mkString("\n")
  }
  def decoder(t: ElmNamedType): String = {
    t.dependent.distinct.map(_.decoder).mkString("\n")
  }

}
