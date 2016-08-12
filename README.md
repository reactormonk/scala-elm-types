# elm-types

Automatic codec generation for elm based on scala case classes. Does currently
NOT support default values correctly.

# Longs

because JS only supports 53 bits of precision in a general JSON parser, use this:

```scala
import elmtype._
import elmtype.derive._
import ElmTypeShapeless._
import scalaz._
import argonaut._
import java.lang.NumberFormatException

object Test {
  implicit val elmlong = RawType[Long]("String", "Encode.string", "Decode.string")
  implicit val longcodec = CodecJson[Long](
    long => Json.jString(long.toString),
    c => c.as[String].flatMap(str =>
      \/.fromTryCatchNonFatal(str.toLong).fold(err => err match {
        case e: NumberFormatException => DecodeResult.fail(e.toString, c.history)
        case e => throw e
      },
        DecodeResult.ok
      )
    )
  )

  implicit val encodeLong = longcodec.Encoder
  implicit val decodeLong = longcodec.Decoder
}
```
