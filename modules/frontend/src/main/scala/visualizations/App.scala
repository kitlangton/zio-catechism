package visualizations

import animator.Animator._
import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalajs.dom.document

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.timers.SetTimeoutHandle

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
          ZioCatechism.main
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
