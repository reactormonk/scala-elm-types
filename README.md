# elm-types

Automatic codec generation for elm based on scala case classes. Does currently
NOT support default values correctly.

# Add to SBT

```sbt
libraryDependencies += "org.reactormonk" %% "elmtypes" % "0.3"
```

# Sample Code

```scala
scala> import elmtype._
import elmtype._

scala> import elmtype.derive._
import elmtype.derive._

scala> import ElmTypeShapeless._
import ElmTypeShapeless._

scala> case class User(id: Int, name: String)
defined class User

scala> sealed trait Protocol
defined trait Protocol

scala> case class Hello(user: User) extends Protocol
defined class Hello

scala> case class Login(user: Option[User]) extends Protocol
defined class Login

scala> case object Boom extends Protocol
defined object Boom

scala> println(AST.code(AST.typeAST(MkElmType[Protocol].elm)).render)
module Codec exposing (..)
import Date exposing (Date)
import Json.Decode.Extra exposing(..)
import Json.Decode as Decode exposing ( field )
import Json.Encode as Encode
import Date.Extra exposing (toUtcIsoString)
type Protocol = ProtocolLogin Login | ProtocolBoom Boom | ProtocolHello Hello
type alias Login = { user : Maybe User }
type alias User = { id : Int, name : String }
type alias Boom = {  }
type alias Hello = { user : User }
decodeProtocol : Decode.Decoder Protocol
decodeProtocol = Decode.oneOf
  [ (field "Login" <| Decode.map ProtocolLogin decodeLogin)
  , (field "Boom" <| Decode.map ProtocolBoom decodeBoom)
  , (field "Hello" <| Decode.map ProtocolHello decodeHello)
  ]
decodeLogin : Decode.Decoder Login
decodeLogin =
  Decode.succeed Login |: (field "user" <| Decode.maybe decodeUser)
decodeUser : Decode.Decoder User
decodeUser =
  Decode.succeed User |: (field "id" <| Decode.int) |: (field "name" <| Decode.string)
decodeBoom : Decode.Decoder Boom
decodeBoom =
  Decode.succeed Boom
decodeHello : Decode.Decoder Hello
decodeHello =
  Decode.succeed Hello |: (field "user" <| decodeUser)

encodeProtocol: Protocol -> Encode.Value
encodeProtocol obj =
  let
    (typefield, inner) = case obj of
      ProtocolLogin obj2 -> ("Login", encodeLogin obj2)
      ProtocolBoom obj2 -> ("Boom", encodeBoom obj2)
      ProtocolHello obj2 -> ("Hello", encodeHello obj2)
  in
    Encode.object [(typefield, inner)]
encodeLogin : Login -> Encode.Value
encodeLogin obj = Encode.object
  [ ("user", Maybe.withDefault Encode.null <| Maybe.map encodeUser obj.user)
  ]
encodeUser : User -> Encode.Value
encodeUser obj = Encode.object
  [ ("id", Encode.int obj.id)
  , ("name", Encode.string obj.name)
  ]
encodeBoom : Boom -> Encode.Value
encodeBoom obj = Encode.object
  [
  ]
encodeHello : Hello -> Encode.Value
encodeHello obj = Encode.object
  [ ("user", encodeUser obj.user)
  ]
```

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
  if (Process("elm-make --yes Main.elm", file("client")).! != 0) {throw new Exception("elm build failed!")}
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
import argonaut._
import java.lang.NumberFormatException
import util._

object Test {
  implicit val elmlong = RawType[Long]("String", "Encode.string", "Decode.string")
  implicit val longcodec = CodecJson[Long](
    long => Json.jString(long.toString),
    c => c.as[String].flatMap(str =>
      Try(str.toLong) match {
        case Failure(e: NumberFormatException) => DecodeResult.fail(e.toString, c.history)
        case Failure(e) => throw e
        case Success(obj) => DecodeResult.ok(obj)
      }
    )
  )

  implicit val encodeLong = longcodec.Encoder
  implicit val decodeLong = longcodec.Decoder
}
```
