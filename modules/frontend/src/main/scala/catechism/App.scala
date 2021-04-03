package catechism

import animator.Animatable
import animator.Animator._
import blogus.components
import blogus.components.codeBlock
import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import catechism.ZioSyntax.ZioOps
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalajs.dom.document
import zio.duration.durationInt
import zio.stream.ZTransducer
import zio.ZEnv

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel, JSImport}
import scala.scalajs.js.timers.SetTimeoutHandle

object StreamCatechism {

  def view: Div = {
    div(
      width("100%"),
      display.flex,
      alignItems.center,
      justifyContent.center,
      div(
        idAttr("ZStream"),
        width("630px"),
        md"##ZStream",
        example_repeatEffect,
        hr(opacity := "0.2"),
        example1,
        hr(opacity := "0.2"),
        example2,
        hr(opacity := "0.2"),
        example3,
        hr(opacity := "0.2"),
        example4,
        hr(opacity := "0.2"),
        example5
      )
    )
  }

  def example_repeatEffect: Div = {
    val stream = VisualStream.numbers
    div(
      width("100%"),
      idAttr("stream.repeatEffect"),
      div(
        a(
          href := s"#stream.repeatEffect",
          components.inlineCode(".repeatEffect"),
        ),
        marginBottom := "1em"
      ),
      margin("24px 0"),
      stream.view,
      codeBlock(
        """
  val numbers: UStream[Int] = for {
    ref <- ZStream.fromEffect(Ref.make(0))
    stream <- ZStream.repeatEffect(ref.getAndUpdate(_ + 1)
  } yield stream
          """.trim,
        marginBottom = false
      ),
      onMountCallback { _ =>
        stream.runDrain.runAsync
      }
    )
  }

  def example1: Div = {
    val stream       = VisualStream.numbers
    val mappedStream = stream.map(_ * 2)
    div(
      width("100%"),
      idAttr("stream.map"),
      div(
        a(
          href := s"#stream.map",
          components.inlineCode(".map"),
        ),
        marginBottom := "1em"
      ),
      margin("24px 0"),
      stream.view,
      codeBlock("numbers.map(_ * 2)", marginBottom = false),
      mappedStream.view,
      onMountCallback { _ =>
        mappedStream.runDrain.runAsync
      }
    )
  }

  def example2: Div = {
    val stream       = VisualStream.letters
    val mappedStream = VisualStream(stream.stream.zipWithIndex)
    div(
      width("100%"),
      idAttr("stream.zipWithIndex"),
      div(
        a(
          href := s"#stream.zipWithIndex",
          components.inlineCode(".zipWithIndex"),
        ),
        marginBottom := "1em"
      ),
      margin("24px 0"),
      stream.view,
      codeBlock("letters.zipWithIndex", marginBottom = false),
      mappedStream.view,
      onMountCallback { _ =>
        mappedStream.runDrain.runAsync
      }
    )
  }

  def example3: Div = {
    val stream       = VisualStream.numbers
    val mappedStream = stream.filter(_ % 2 == 0)
    div(
      width("100%"),
      idAttr("stream.filter"),
      div(
        a(
          href := s"#stream.filter",
          components.inlineCode(".filter"),
        ),
        marginBottom := "1em"
      ),
      margin("24px 0"),
      stream.view,
      codeBlock("numbers.filter(_ % 2 == 0)", marginBottom = false),
      mappedStream.view,
      onMountCallback { _ =>
        mappedStream.runDrain.runAsync
      }
    )
  }

  def example4: Div = {
    val stream = VisualStream.letters
    val mappedStream = VisualStream(
      stream.stream.zipWithPreviousAndNext.collect {
        case (Some(x), y, Some(z)) => x + y + z
      }
    )
    div(
      width("100%"),
      idAttr("stream.zipWithPreviousAndNext"),
      div(
        a(
          href := s"#stream.zipWithPreviousAndNext",
          components.inlineCode(".zipWithPreviousAndNext")
        ),
        marginBottom := "1em"
      ),
      margin("24px 0"),
      stream.view,
      codeBlock("""
letters.zipWithPreviousAndNext.collect {
  case (Some(a), b, Some(c)) => a + b + c
}
              """.trim,
                marginBottom = false),
      mappedStream.view,
      onMountCallback { _ =>
        mappedStream.runDrain.runAsync
      }
    )
  }

  def example5: Div = {
    val stream = VisualStream.randomInt
    val mappedStream = VisualStream(
      stream.stream.grouped(3).map(_.sum)
    )
    div(
      width("100%"),
      idAttr("stream.grouped"),
      div(
        a(
          href := s"#stream.grouped",
          components.inlineCode(".grouped")
        ),
        marginBottom := "1em"
      ),
      margin("24px 0"),
      stream.view,
      codeBlock("randomInts.grouped(3).map(_.sum)", marginBottom = false),
      mappedStream.view,
      onMountCallback { _ =>
        mappedStream.runDrain.runAsync
      }
    )
  }

}

@js.native
@JSImport("stylesheets/main.scss", JSImport.Namespace)
object Css extends js.Any

object App {
  var windowSize: Var[(Double, Double)] = Var((dom.window.innerWidth, dom.window.innerHeight))

  val css        = Css
  val exampleVar = Var(1)

  def main(args: Array[String]): Unit = {
    val container = document.getElementById("app") // This div, its id and contents are defined in index-fastopt.html/index-fullopt.html files
    var ignoredRoot =
      render(
        container,
        div(
          ZioCatechism.main,
        )
      )

  }

  def every(rateMs: Int = 300): Signal[Int] = {
    val valueVar = Var(0)

    def step(): SetTimeoutHandle =
      scalajs.js.timers.setTimeout(rateMs) {
        valueVar.update(_ + 1)
        step()
      }

    step()

    valueVar.signal
  }

  def random[A: Animatable](value: => A, rateMs: => Int = 300): Signal[A] = {
    val valueVar = Var(value)

    def step(): SetTimeoutHandle =
      scalajs.js.timers.setTimeout(rateMs) {
        valueVar.set(value)
        step()
      }

    step()

    spring(valueVar.signal, stiffness = 200, damping = 18)
  }
}
