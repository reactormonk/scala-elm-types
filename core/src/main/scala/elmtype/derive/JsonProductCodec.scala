package elmtype
package derive

trait JsonProductCodec {
  def emptyObject[A](): ProductType[A]
  def fieldType[A](name: String, elmType: ElmType[A]): ElmField[A]
}

object JsonProductCodec {
  val obj: JsonProductCodec = new JsonProductObjCodec
  def adapt(f: String => String): JsonProductCodec = new JsonProductObjCodec {
    override def toJsonName(name: String) = f(name)
  }
}

trait JsonProductCodecFor[P] {
  def codec: JsonProductCodec
}

object JsonProductCodecFor {
  def apply[S](codec0: JsonProductCodec): JsonProductCodecFor[S] =
    new JsonProductCodecFor[S] {
      def codec = codec0
    }

  implicit def default[T]: JsonProductCodecFor[T] =
    JsonProductCodecFor(JsonProductCodec.obj)
}

class JsonProductObjCodec extends JsonProductCodec {
  def toJsonName(name: String) = name

  def emptyObject[A](): ProductType[A] = ProductType(List())
  def fieldType[A](name: String, elmType: ElmType[A]): ElmField[A] = ElmField(toJsonName(name), elmType)
}
