package elmtype
package derive

trait JsonSumCodec {
  def emptyObject[T](): UnnamedSumType[T]
  def fieldType[T](name: String, elmType: ElmType[T]): ElmField[T]
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
  def emptyObject[T](): UnnamedSumType[T] = UnnamedSumType[T](List(), None)
  def fieldType[T](name: String, elmType: ElmType[T]): ElmField[T] = ElmField[T](name, elmType)
}

class JsonSumTypeFieldCodec extends JsonSumCodec {

  def toJsonName(name: String) = name
  def typeField: String = "type"

  def emptyObject[A](): UnnamedSumType[A] = UnnamedSumType[A](List(), Some(typeField))
  def fieldType[A](name: String, elmType: ElmType[A]): ElmField[A] = ElmField(toJsonName(name), elmType)
}
