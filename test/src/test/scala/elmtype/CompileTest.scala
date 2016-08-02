package elmtype

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import utest._
import elmtype._
import elmtype.derive._
import ElmTypeShapeless._
import collection.JavaConverters._
import sys.process._
import language.implicitConversions

object CompileTest extends TestSuite {
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

  def compileExternal[T](elmType: CombinedType[T]) = {
    val ast = AST.typeAST(elmType)
    val code = AST.code(ast)
    val path = Files.createTempDirectory("elmtypes")
    Files.write(path.resolve("Main.elm"), List(code).asJava, StandardCharsets.UTF_8)
    Files.write(path.resolve("elm-package.json"), List(packages).asJava, StandardCharsets.UTF_8)
    val exit = Process("elm make --yes Main.elm", path.toFile).!
      assert(exit == 0)
  }

  val tests = TestSuite {
    'basic - {
      compileExternal(MkElmType[Basic].elm)
    }
    'sum - {
      compileExternal(MkElmType[Sealed].elm)
    }
    'listy - {
      implicitly[ValidSubType[List[Int]]]
      implicitly[ValidSubType[Option[String]]]
      compileExternal(MkElmType[Listy].elm)
    }
    'datey - {
      compileExternal(MkElmType[Datey].elm)
    }
  }
}
