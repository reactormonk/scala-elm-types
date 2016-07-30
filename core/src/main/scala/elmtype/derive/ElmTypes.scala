package elmtype

sealed trait ElmType[T]
case class SumType[T](types: List[ElmType[_]], typefield: Option[String]) extends ElmType[T] {
  def map[S](ev: T => S) = SumType[S](types, typefield)
}
case class ProductType[T](types: List[ElmField[_]]) extends ElmType[T] {
  def map[S](ev: T => S) = ProductType[S](types)
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
}
