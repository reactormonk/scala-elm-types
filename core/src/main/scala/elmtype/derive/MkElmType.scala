package elmtype
package derive

import shapeless._
import shapeless.labelled.{ field, FieldType }

trait MkElmType[T] {
  def elm: ElmType[T]
}

object MkElmType {
  def apply[T](implicit elmType: MkElmType[T]): MkElmType[T] = elmType

  implicit def product[P]
   (implicit
     underlying: ProductElmType[P],
     codecFor: JsonProductCodecFor[P],
     typeable: Typeable[P]
   ): MkElmType[P] =
    new MkElmType[P] {
      def elm = ProductType(typeable, underlying(codecFor.codec))
    }

  implicit def sum[S]
   (implicit
     underlying: SumElmType[S],
     codecFor: JsonSumCodecFor[S],
     typeable: Typeable[S]
   ): MkElmType[S] =
    new MkElmType[S] {
      def elm = SumType(typeable, underlying(codecFor.codec))
    }
}

trait ProductElmType[P] {
  def apply(productCodec: JsonProductCodec): UnnamedProductType[P]
}

object ProductElmType {
  def apply[P](implicit elmType: ProductElmType[P]): ProductElmType[P] = elmType

  def instance[P](f: JsonProductCodec => UnnamedProductType[P]): ProductElmType[P] =
    new ProductElmType[P] {
      def apply(productCodec: JsonProductCodec) =
        f(productCodec)
    }

  implicit def generic[P, L <: HList]
   (implicit
     gen: LabelledGeneric.Aux[P, L],
     underlying: Lazy[HListProductElmType[L]]
   ): ProductElmType[P] =
    instance { (productCodec) =>
      underlying.value(productCodec).map(gen.from)
    }
}

trait HListProductElmType[L <: HList] {
  def apply(productCodec: JsonProductCodec): UnnamedProductType[L]
}

object HListProductElmType {
  def apply[P, L <: HList](implicit elmType: HListProductElmType[L]): HListProductElmType[L] =
    elmType

  def instance[L <: HList](f: JsonProductCodec => UnnamedProductType[L]): HListProductElmType[L] =
    new HListProductElmType[L] {
      def apply(productCodec: JsonProductCodec) =
        f(productCodec)
    }

  implicit val hnil: HListProductElmType[HNil] =
    instance { (productCodec) => productCodec.emptyObject() }

  implicit def hcons[P, K <: Symbol, H, T <: HList]
   (implicit
     key: Witness.Aux[K],
     headDecode: Strict[ElmType[H]],
     tailDecode: HListProductElmType[T]
   ): HListProductElmType[FieldType[K, H] :: T] =
    instance { (productCodec) =>
      lazy val tailDecode0 = tailDecode(productCodec)
      val current = productCodec.fieldType(key.value.name, headDecode.value)
      tailDecode0.copy(types = current :: tailDecode0.types)
    }
}

trait CoproductSumElmType[C <: Coproduct] {
  def apply(sumCodec: JsonSumCodec): UnnamedSumType[C]
}

object CoproductSumElmType {
  def apply[C <: Coproduct](implicit elmtype: CoproductSumElmType[C]): CoproductSumElmType[C] =
    elmtype

  def instance[C <: Coproduct](f: JsonSumCodec => UnnamedSumType[C]): CoproductSumElmType[C] =
    new CoproductSumElmType[C] {
      def apply(sumCodec: JsonSumCodec) =
        f(sumCodec)
    }

  implicit val cnil: CoproductSumElmType[CNil] =
    instance { sumCodec =>
      sumCodec.emptyObject()
    }

  implicit def ccons[K <: Symbol, H, T <: Coproduct]
   (implicit
     key: Witness.Aux[K],
     headDecode: Lazy[ElmType[H]],
     tailDecode: CoproductSumElmType[T]
   ): CoproductSumElmType[FieldType[K, H] :+: T] =
    instance { sumCodec =>
      lazy val tailDecode0 = tailDecode(sumCodec)
      val current = sumCodec.fieldType(key.value.name, headDecode.value)
      tailDecode0.copy(types = current :: tailDecode0.types)
    }
}

trait SumElmType[S] {
  def apply(sumCodec: JsonSumCodec): UnnamedSumType[S]
}

object SumElmType {
  def apply[S](implicit elmtype: SumElmType[S]): SumElmType[S] = elmtype

  def instance[S](f: JsonSumCodec => UnnamedSumType[S]): SumElmType[S] =
    new SumElmType[S] {
      def apply(sumCodec: JsonSumCodec) =
        f(sumCodec)
    }

  implicit def union[U <: Coproduct]
   (implicit
     underlying: CoproductSumElmType[U]
   ): SumElmType[U] =
    instance { sumCodec =>
      underlying(sumCodec)
    }

  implicit def generic[S, C <: Coproduct]
   (implicit
     gen: LabelledGeneric.Aux[S, C],
     underlying: Strict[CoproductSumElmType[C]]
   ): SumElmType[S] =
    instance { sumCodec =>
      underlying.value(sumCodec).map(gen.from)
    }
}
