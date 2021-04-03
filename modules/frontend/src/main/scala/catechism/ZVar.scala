package catechism

import animator.Animator.spring
import catechism.ObservableSyntax._
import com.raquo.laminar.api.L.{Ref => _, _}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import zio._
import zio.clock.Clock
import zio.duration._

trait Renderable {
  def render: ReactiveHtmlElement.Base
  def reset: UIO[Unit]
}

case class ZVar[E, A] private (variable: Var[A],
                               isResult: Boolean = false,
                               reset0: (Var[A], Var[Option[E]]) => UIO[Unit] = (_: Var[A], _: Var[Option[E]]) =>
                                 UIO.unit)
    extends Renderable {
  private val isUpdating  = Var(false)
  private val $isUpdating = isUpdating.signal

  private val isInterrupted  = Var(false)
  private val $isInterrupted = isInterrupted.signal

  val error       = Var(Option.empty[E])
  lazy val $error = error.signal

  val ref: Ref[A] = null

  def reset: UIO[Unit] = reset0(variable, error)

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

  def render: ReactiveHtmlElement[html.Div] = {
    div(
      padding := "8px",
      opacity <-- $opacity,
      Option.when(!isResult)(marginRight := "12px"),
      justifyContent.center,
      borderRadius := "4px",
      background <-- $background,
      display.flex,
      overflow.hidden,
      div(
        child.text <-- $text
      ),
      onMountBind { el =>
        width <-- $text.mapTo(Math.max(46.0, el.thisNode.ref.firstElementChild.scrollWidth.toDouble + 16)).spring.px
      }
    )
  }

  val $text = variable.signal.combineWith($error).map {
    case (_, Some(_))      => "☠️"
    case (Some(a), _)      => a.toString
    case (_: None.type, _) => "⁃"
    case (a, _)            => a.toString
  }

  private def $opacity: Signal[Double] = {
    variable match {
      case _: Var[Option[_]] =>
        spring(signal.combineWith($error).map {
          case (_, Some(_))                   => 1.0
          case (a: Option[_], _) if a.isEmpty => 0.3
          case _                              => 1.0
        })
      case _ =>
        Val(1.0)
    }
  }

  private val $background = spring($isUpdating.combineWith($isInterrupted).combineWith($error).map {
    case (updating, interrupted, error) =>
      val alpha = if (updating) 1.0 else 0.4
      if (error.isDefined)
        (180.0, 80.0, 80.0, alpha)
      else if (interrupted)
        (160.0, 120.0, 80.0, alpha)
      else if (isResult)
        (80.0, 160.0, 80.0, alpha)
      else
        (80.0, 80.0, 160.0, alpha)
  }).map { case (r, g, b, a) => s"rgba($r,$g,$b,$a)" }

}

object ZVar {
  type UVar[A] = ZVar[Nothing, A]
  def apply[E, A](value: A): ZVar[E, A] = ZVar[E, A](variable = Var(value))
  def result[A]: ZVar[Nothing, Option[A]] =
    resultE[Nothing, A]

  def resultE[E, A]: ZVar[E, Option[A]] =
    ZVar(variable = Var(Option.empty[A]),
         isResult = true,
         reset0 = (variable, error) =>
           UIO {
             variable.set(Option.empty[A])
             error.set(Option.empty[E])
         })
}
