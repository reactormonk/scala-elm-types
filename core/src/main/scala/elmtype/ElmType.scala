package elmtype

import elmtype.derive._

object ElmTypeShapeless
    extends ElmTypes with DerivedInstances {

  object Cached
    extends CachedDerivedInstances
}
