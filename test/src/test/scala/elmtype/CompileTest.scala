package elmtype

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import java.time.format.DateTimeParseException
import java.lang.NumberFormatException
import java.io.ByteArrayInputStream
import utest._
import elmtype._
import elmtype.derive._
import ElmTypeShapeless._
import collection.JavaConverters._
import sys.process._
import language.implicitConversions
import shapeless._
import argonaut._, Argonaut._, ArgonautShapeless._
import scalaz._
import java.time.Instant

object CompileTest extends TestSuite {

  implicit val instantCodec: CodecJson[Instant] = CodecJson(
    (instant: Instant) => Json.jString(instant.toString),
    c => c.as[String].flatMap(str =>
      \/.fromTryCatchNonFatal(Instant.parse(str)).fold(err => err match {
        case e: DateTimeParseException => DecodeResult.fail(e.toString, c.history)
        case e => throw e
      },
        DecodeResult.ok
      )
    )
  )

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

  val packages = """
{
  "version": "1.0.0",
  "summary": "helpful summary of your project, less than 80 characters",
  "repository": "https://github.com/user/project.git",
  "license": "BSD3",
  "source-directories": [
    "."
  ],
  "exposed-modules": [],
  "dependencies": {
    "elm-lang/core": "4.0.3 <= v < 5.0.0",
    "elm-lang/html": "1.1.0 <= v < 2.0.0",
    "elm-community/json-extra": "1.0.0 <= v < 2.0.0",
    "justinmimbs/elm-date-extra": "2.0.0 <= v < 3.0.0"
  },
  "elm-version": "0.17.1 <= v < 0.18.0"
}
"""

  val backslash = "\\"
  def elmTestRunner(encoderName: String, decoderName: String) = s"""
port module Main exposing (..)

import Html exposing (..)
import Html.App as App
import Codec exposing (..)
import Json.Encode exposing (..)
import Json.Decode exposing (decodeString)
import Result exposing (..)

type Msg = Json String

port jsons: (String -> msg) -> Sub msg

main : Program Never
main =
    App.programWithFlags
        { init = ${backslash}_ -> ((), Cmd.none)
        , update = ${backslash}(Json msg) _ -> (Debug.log (testJson msg) <| (), Cmd.none)
        , view = ${backslash}() -> Html.text "Check the console for useful output!"
        , subscriptions = subscriptions
        }

testJson: String -> String
testJson msg = case (map (Codec.${encoderName} >> (encode 0)) (decodeString Codec.${decoderName} msg)) of
             Err msg -> msg
             Ok str -> str

subscriptions : () -> Sub Msg
subscriptions model =
  jsons Json
"""

  val nodeRunner = """
const elm = require("./elm.js");
const readline = require("readline");

var app = elm.Main.worker();

process.stdin.resume();

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', function(line){
    app.ports.jsons.send(line);
})

process.stdout.on('error', function(err) {
  if (err.code === 'EPIPE') return process.exit();
  process.emit('error', err);
});
"""

  def compileExternal[T: EncodeJson: DecodeJson](elmType: CombinedType[T], obj: T): List[T] = {
    val ast = AST.typeAST(elmType)
    val code = AST.code(ast).render
    val path = Files.createTempDirectory("elmtypes")
    println(s"Writing $obj in $path")
    Files.write(path.resolve("Codec.elm"), List(code).asJava, StandardCharsets.UTF_8)
    Files.write(path.resolve("elm-package.json"), List(packages).asJava, StandardCharsets.UTF_8)
    Files.write(path.resolve("Main.elm"), List(elmTestRunner(ast.encoderName, ast.decoderName)).asJava, StandardCharsets.UTF_8)
    val exit = Process("elm make --yes Main.elm --output elm.js", path.toFile).!
    assert(exit == 0)
    Files.write(path.resolve("test.js"), List(nodeRunner).asJava, StandardCharsets.UTF_8)
    val json = (Process("node test.js", path.toFile) #< new ByteArrayInputStream(obj.asJson.nospaces.getBytes(StandardCharsets.UTF_8))).lineStream.map(_.dropRight(4))
    json.toList.map(j => Parse.decodeEither[T](j).leftMap[Nothing](err => throw new Exception(err)).merge)
  }

  val tests = TestSuite {
    'basic - {
      val obj = Basic(23, "foo")
      val compiled = compileExternal(MkElmType[Basic].elm, obj)
      assert(compiled(0) == obj)
    }
    'sum - {
      val s = S("foo")
      val compiledS = compileExternal(MkElmType[Sealed].elm, s)
      assert(compiledS(0) == s)
      val i = I(42)
      val compiledI = compileExternal(MkElmType[Sealed].elm, i)
      assert(compiledI(0) == i)
    }
    'listy - {
      val l = Listy(List(23, 42), Some("bar"))
      val compiled = compileExternal(MkElmType[Listy].elm, l)
      assert(compiled(0) == l)
    }
    'datey - {
      val d = Datey(Instant.parse("2007-12-03T10:15:30.00Z"))
      val compiled = compileExternal(MkElmType[Datey].elm, d)
      assert(compiled(0) == d)
    }
    'longy - {
      val l = Longy(2893819230231232123L)
      val compiled = compileExternal(MkElmType[Longy].elm, l)
      assert(compiled(0) == l)
    }
    'nested - {
      val n = Nested(2, Basic(23, "foo"))
      val compiled = compileExternal(MkElmType[Nested].elm, n)
      assert(compiled(0) == n)
    }
    'alias - {
      val a = Alias(Identifier[Character](23, "foo"))
      val compiled = compileExternal(implicitly[CombinedType[Alias]], a)
      assert(compiled(0) == a)
    }
    'nestedast - {
      val n = NestedS("foo")
      val compiled = compileExternal(MkElmType[NestedAST].elm, n)
      assert(compiled(0) == n)
    }
    'caseobject - {
      val o = CaseObject
      val compiled = compileExternal(MkElmType[CaseObjectAST].elm, o)
      assert(compiled(0) == o)
    }
  }
}
