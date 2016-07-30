package elmtype

import shapeless._

sealed trait ElmTypeAST {
  def dependent: List[ElmNamedType]
  def name: String
}
sealed trait ElmNamedType extends ElmTypeAST
case class ASTAlias(name: String, fields: List[ASTField], innerDependent: List[ElmNamedType]) extends ElmNamedType {
  val dependent = this :: innerDependent
}
case class ASTSum(name: String, fields: List[ASTField], innerDependent: List[ElmNamedType]) extends ElmNamedType {
  val dependent = this :: innerDependent
}
case class ASTField(name: String, typeDcl: String, dependent: List[ElmNamedType]) extends ElmTypeAST
case class RawAST(typeDecl: String, dependent: List[ElmNamedType]) extends ElmTypeAST {
  val name = typeDecl
}

case class UnnamedSumType[T](types: List[ElmField[_]], typefield: Option[String]) {
  def map[S](ev: T => S) = UnnamedSumType[S](types, typefield)
}
case class UnnamedProductType[T](types: List[ElmField[_]]) {
  def map[S](ev: T => S) = UnnamedProductType[S](types)
}
sealed trait ElmType[T]
case class SumType[T](typeable: Typeable[T], unnamed: UnnamedSumType[T]) extends ElmType[T] {
  val types = unnamed.types
  val typefield = unnamed.typefield
}
case class ProductType[T](typeable: Typeable[T], unnamed: UnnamedProductType[T]) extends ElmType[T] {
  val types = unnamed.types
}
sealed trait DirectType[T] extends ElmType[T]
case class ElmField[T](name: String, innerType: ElmType[T]) extends DirectType[T]
case class RawType[T](name: String, encoder: String, decoder: String) extends DirectType[T]
case class HigherType[T[_], I](name: String, encoder: String, decoder: String, innerType: ElmType[I]) extends DirectType[T[ElmType[I]]]

object ElmType {
  implicit val elmstring = RawType[String]("String", "string", "string")
  implicit val elmint = RawType[Int]("Int", "int", "int")
  implicit val elmfloat = RawType[Float]("Float", "float", "float")
  implicit val elmlong = RawType[Long]("String", "string", "string")
  implicit val elmbool = RawType[Boolean]("Bool", "bool", "bool")
  implicit def elmlist[T](innerType: ElmType[T]): DirectType[List[ElmType[T]]] = HigherType("List", "list", "list", innerType)
  implicit def elmoption[T](innerType: ElmType[T]): DirectType[Option[ElmType[T]]] = HigherType("Maybe", "maybe", "maybe", innerType)

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
