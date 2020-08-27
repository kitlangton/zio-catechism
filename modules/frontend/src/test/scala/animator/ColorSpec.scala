package animator

import zio.test._
import zio.test.Assertion._

object ColorSpec extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Color")(
    suite("mixes additively with other Colors")(
      test("red + blue = magenta")(assert(Color.red + Color.blue)(equalTo(Color.magenta))),
      test("red + green = yellow")(assert(Color.red + Color.green)(equalTo(Color.yellow))),
      test("blue + green = cyan")(assert(Color.blue + Color.green)(equalTo(Color.cyan))),
    ),
  )
}
