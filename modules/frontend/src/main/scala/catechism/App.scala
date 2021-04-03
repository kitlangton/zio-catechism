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

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
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
        example1,
        hr(opacity := "0.2"),
        example2,
        hr(opacity := "0.2"),
        example3
      )
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
}
@JSExportTopLevel("App")
object App {
  var windowSize: Var[(Double, Double)] = Var((dom.window.innerWidth, dom.window.innerHeight))

  val exampleVar = Var(1)
  @JSExport
  def start(): Unit = {
    val container = document.getElementById("app-container") // This div, its id and contents are defined in index-fastopt.html/index-fullopt.html files
    var ignoredRoot =
      render(
        container,
        div(
          ZioCatechism.main,
//          formula.Example.body
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
