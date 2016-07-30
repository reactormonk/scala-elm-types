package elmtype.derive

import elmtype.{ ElmType }

import shapeless.{ Cached, LowPriority, Strict, Widen, Witness }

trait DerivedInstances {

  implicit def derivedElmType[T]
   (implicit
     ev: LowPriority,
     underlying: Strict[MkElmType[T]]
   ): ElmType[T] =
    underlying.value.apply
}

trait CachedDerivedInstances {

  implicit def cachedDerivedElmType[T]
   (implicit
     ev: LowPriority,
     underlying: Cached[Strict[MkElmType[T]]]
   ): ElmType[T] =
    underlying.value.value.apply
}
