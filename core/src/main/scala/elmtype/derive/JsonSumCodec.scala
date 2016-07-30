package elmtype
package derive

trait JsonSumCodec {
  def emptyObject[T](): SumType[T]
  def fieldType[T](name: String, elmType: ElmType[T]): SumType[T]
}

trait JsonSumCodecFor[S] {
  def codec: JsonSumCodec
}

object JsonSumCodecFor {
  def apply[S](codec0: JsonSumCodec): JsonSumCodecFor[S] =
    new JsonSumCodecFor[S] {
      def codec = codec0
    }

  implicit def default[T]: JsonSumCodecFor[T] =
    JsonSumCodecFor(JsonSumCodec.obj)
}

object JsonSumCodec {
  val obj: JsonSumCodec = new JsonSumObjCodec
  val typeField: JsonSumCodec = new JsonSumTypeFieldCodec
}

class JsonSumObjCodec extends JsonSumCodec {
  def emptyObject[T](): SumType[T] = SumType[T](List(), None)
  def fieldType[T](name: String, elmType: ElmType[T]): SumType[T] = SumType(List(ElmField[T](name, elmType)), None)
}

class JsonSumTypeFieldCodec extends JsonSumCodec {

  def toJsonName(name: String) = name
  def typeField: String = "type"

  def emptyObject[A](): SumType[A] = SumType[A](List(), Some(typeField))
  def fieldType[A](name: String, elmType: ElmType[A]): SumType[A] = SumType(List(ElmField(toJsonName(name), elmType)), Some(typeField))
}
