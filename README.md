# elm-types

Automatic codec generation for elm based on scala case classes. Does currently
NOT support default values correctly.

# Usage

To specify which codecs to use:

```scala
import elmtype._
import elmtype.derive._
import ElmTypeShapeless._
import shapeless._

sealed trait ClientToServer
case class Ping(message: String) extends ClientToServer

sealed trait ServerToClient
case class Pong(message: String) extends ServerToClient

object Elm {
  val types = ToElmTypes[ClientToServer :: ServerToClient :: HNil].apply
}

object ElmTypes extends ElmTypeMain(Elm.types)
```

To compile the elm code in your `build.sbt`:

```scala
val compileElm = taskKey[File]("Compile the elm into an index.html")

(compileElm in client) := {
  val codec = (baseDirectory in client).value / "Codec.elm"
  (runner in (shared, run)).value.run("ElmTypes", Attributed.data((fullClasspath in shared in Compile).value), Seq(codec.toString), streams.value.log)
  if (Process("elm-make Main.elm", file("client")).! != 0) {throw new Exception("elm build failed!")}
  (baseDirectory in client).value / "index.html"
}
```

Then add the result of `(compileElm in client)` to your assets.

Dependencies to add:

```
"elm-community/json-extra": "1.0.0 <= v < 2.0.0",
"justinmimbs/elm-date-extra": "2.0.0 <= v < 3.0.0"
```

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
