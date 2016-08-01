package elmtype.derive

import elmtype.{ CombinedType }

import shapeless.{ Cached, LowPriority, Strict, Widen, Witness }

trait DerivedInstances {

  implicit def derivedElmType[T]
   (implicit
     ev: LowPriority,
     underlying: Strict[MkElmType[T]]
   ): CombinedType[T] =
    underlying.value.elm
}

trait CachedDerivedInstances {

  implicit def cachedDerivedElmType[T]
   (implicit
     ev: LowPriority,
     underlying: Cached[Strict[MkElmType[T]]]
   ): CombinedType[T] =
    underlying.value.value.elm
}
