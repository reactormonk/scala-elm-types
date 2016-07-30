package elmtype

import shapeless._

sealed trait ElmTypeAST {
  def dependent: List[ElmNamedType]
  def name: String
}
sealed trait ElmNamedType extends ElmTypeAST {
  def render: String
}
case class ASTAlias(name: String, fields: List[ASTField], innerDependent: List[ElmNamedType]) extends ElmNamedType {
  val dependent = this :: innerDependent
  val renderedFields = fields.map({ f => s"${f.name} : ${f.typeDecl}"})
  val render = (s"type alias $name = { ${renderedFields.mkString(", ") } }" :: innerDependent.distinct.map(_.render)).mkString("\n")
}
case class ASTSum(name: String, fields: List[ASTField], innerDependent: List[ElmNamedType]) extends ElmNamedType {
  val dependent = this :: innerDependent
  val renderedFields = fields.map({ f => s"${name}${f.name} ${f.typeDecl}"})
  val render = (s"type $name = ${renderedFields.mkString(" | ")}" :: innerDependent.distinct.map(_.render)).mkString("\n")
}
case class ASTField(name: String, typeDecl: String, dependent: List[ElmNamedType]) extends ElmTypeAST {
}
case class RawAST(typeDecl: String, dependent: List[ElmNamedType]) extends ElmTypeAST {
  val name = typeDecl
}

object AST {
    def typeAST[T](s: SumType[T]): ASTSum = {
    val inner = s.types.map(typeAST)
    val dependents = inner.flatMap(_.dependent)
    ASTSum(s.typeable.describe, inner, dependents)
  }
  def typeAST(p: ProductType[_]): ASTAlias = {
    val inner = p.types.map(typeAST)
    val dependents = inner.flatMap(_.dependent)
    ASTAlias(p.typeable.describe, inner, dependents)
  }
  def typeAST(e: ElmField[_]): ASTField = {
    val inner = typeAST(e.innerType)
    ASTField(e.name, inner.name, inner.dependent)
  }
  def typeAST(r: RawType[_]): RawAST = {
    RawAST(r.name, List())
  }
  def typeAST[T[_]](h: HigherType[T, _]): RawAST = {
    val inner = typeAST(h.innerType)
    RawAST(s"${h.name} ${inner.name}", inner.dependent)
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

}
