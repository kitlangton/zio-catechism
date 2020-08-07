package starter

import scalajsjest.JestGlobal
import scalajsjest.JestRunner

import scala.scalajs.js

object TestRunner {

  def main(args: Array[String]): Unit = {
//    val originalDescribe = JestGlobal.describe
//    JestGlobal.describe = (str: String, function: js.Function0[Any]) => {
//      originalDescribe(str, function)
//      js.undefined
//    }
    JestRunner.run()
  }

}
