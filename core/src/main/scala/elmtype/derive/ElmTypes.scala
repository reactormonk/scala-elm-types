package elmtype

import shapeless._
import java.time.Instant
import language.implicitConversions

case class UnnamedSumType[T](types: List[ElmField[_]], typefield: Option[String]) {
  def map[S](ev: T => S) = UnnamedSumType[S](types, typefield)
}
case class UnnamedProductType[T](types: List[ElmField[_]]) {
  def map[S](ev: T => S) = UnnamedProductType[S](types)
}

sealed trait ElmType[T]
sealed trait ValidSubType[T] extends ElmType[T]
sealed trait CombinedType[T] extends ElmType[T] with ValidSubType[T]
case class SumType[T](typeable: Typeable[T], unnamed: UnnamedSumType[T]) extends CombinedType[T] {
  val types = unnamed.types
  val typefield = unnamed.typefield
}
case class ProductType[T](typeable: Typeable[T], unnamed: UnnamedProductType[T]) extends CombinedType[T] {
  val types = unnamed.types
}
sealed trait DirectType[T] extends ElmType[T]
case class ElmField[T](name: String, innerType: ValidSubType[T]) extends DirectType[T]
case class RawType[T](name: String, encoder: String, decoder: String) extends DirectType[T] with ValidSubType[T]
case class HigherType[T[_], I](name: String, encoder: String, decoder: String, innerType: ValidSubType[I]) extends DirectType[T[I]] with ValidSubType[T[I]]

trait ElmTypes {
  implicit val elmstring = RawType[String]("String", "Encode.string", "Decode.string")
  implicit val elmint = RawType[Int]("Int", "Encode.int", "Decode.int")
  implicit val elmfloat = RawType[Float]("Float", "Encode.float", "Decode.float")
  implicit val elmlong = RawType[Long]("String", "Encode.string", "Decode.string")
  implicit val elmbool = RawType[Boolean]("Bool", "Encode.bool", "Decode.bool")
  implicit val elminstant = RawType[Instant]("Date", "Encode.string <| toUtcIsoString", "date")
  implicit def elmlist[T](innerType: ValidSubType[T]): ValidSubType[List[T]] = HigherType[List, T]("List", "Encode.list", "Decode.list", innerType)
  implicit def elmoption[T](innerType: ValidSubType[T]): ValidSubType[Option[T]] = HigherType[Option, T]("Maybe", "Encode.maybe", "Decode.maybe", innerType)
}
