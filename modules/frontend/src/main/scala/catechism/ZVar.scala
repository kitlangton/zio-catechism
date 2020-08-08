package catechism

import animator.Animator.spring
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import zio.clock.Clock
import zio.duration._
import zio.{Ref, UIO, URIO}

case class ZVar[A] private (variable: Var[A], isResult: Boolean = false) {

  private val isUpdating  = Var(false)
  private val $isUpdating = isUpdating.signal

  private val isInterrupted  = Var(false)
  private val $isInterrupted = isInterrupted.signal

  val ref: Ref[A] = null

  def withUpdate(f: => Unit): URIO[Clock, Unit] =
    (UIO(isUpdating.set(true)) *>
      UIO(f).delay(50.millis) *>
      UIO(isUpdating.set(false)).delay(300.millis))
      .onInterrupt(UIO(isUpdating.set(false)))

  def set(a: A): URIO[Clock, Unit]            = withUpdate { variable.set(a) }
  def update(f: A => A): URIO[Clock, Unit]    = withUpdate { variable.update(f) }
  def updateAndGet(f: A => A): URIO[Clock, A] = withUpdate { variable.update(f) } *> get
  def get: UIO[A]                             = UIO(variable.now())
  def signal: StrictSignal[A]                 = variable.signal

  def interrupt(bool: Boolean = true): UIO[Unit] = UIO(isInterrupted.set(bool))

  def render: ReactiveHtmlElement[html.Div] =
    div(
      padding := "8px",
      opacity <-- $opacity,
      Option.when(!isResult)(marginRight := "12px"),
      borderRadius := "4px",
      background <-- $background,
      child.text <-- variable.signal.map {
        case Some(a)      => a.toString
        case _: None.type => "_"
        case a            => a.toString
      }
    )

  private def $opacity: Signal[Double] = {
    variable match {
      case _: Var[Option[_]] =>
        spring(signal.map {
          case a: Option[_] if a.isEmpty => 0.5
          case _                         => 1.0
        })
      case _ =>
        Val(1.0)
    }
  }

  private val $background = spring($isUpdating.combineWith($isInterrupted).map {
    case (updating, interrupted) =>
      val alpha = if (updating) 1.0 else 0.4
      if (interrupted)
        (160.0, 120.0, 80.0, alpha)
      else if (isResult)
        (80.0, 160.0, 80.0, alpha)
      else
        (80.0, 80.0, 160.0, alpha)
  }).map { case (r, g, b, a) => s"rgba($r,$g,$b,$a)" }

}

object ZVar {
  def apply[A](value: A): ZVar[A]  = ZVar[A](variable = Var(value))
  def result[A](value: A): ZVar[A] = ZVar[A](variable = Var(value), isResult = true)
}
