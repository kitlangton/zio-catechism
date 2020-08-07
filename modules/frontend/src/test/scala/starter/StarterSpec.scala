package starter

import com.raquo.domtestutils.matching.RuleImplicits
import com.raquo.laminar.api.L._
import scalajsjest.JestSuite

class StarterSpec extends JestSuite with JestMountOps with RuleImplicits {

  beforeEach {
    resetDOM()
  }

  test("does something") {
    mount(div("Hello!"))
    expectNode(div like ("Hello!"))
  }

  test("fails") {
    mount(span("Hello!"))
    expectNode(span like ("Hello2!"))
  }

}
