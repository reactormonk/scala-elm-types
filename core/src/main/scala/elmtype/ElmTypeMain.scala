package elmtype

import elmtype._
import java.io.FileOutputStream
import shapeless._
import shapeless.ops.hlist.LiftAll
import shapeless.ops.hlist.ToTraversable

case class ElmTypeMain(list: List[CombinedType[_]]) {
  def main(args: Array[String]): Unit = {
    val target = args(0) match {
      case "-" => System.out
      case x => new FileOutputStream(x)
    }
    val data = list.map(AST.typeAST).map(AST.code).mkString("\n")
    target.write(io.Codec.toUTF8(data))
    target.close
  }
}

case class ToElmTypes[L <: HList]() {
  def apply[Lift <: HList](implicit ev1: LiftAll.Aux[CombinedType, L, Lift], ev2: ToTraversable[Lift, List]): ev2.Out =
    ev2(ev1.instances)
}
