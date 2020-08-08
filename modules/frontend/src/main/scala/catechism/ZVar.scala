package catechism

import animator.Animation._
import animator._
import animator.Animator.spring
import com.raquo.laminar.api.L.{Ref => _, _}
import com.raquo.laminar.ext.CSS.fontVariant
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import catechism.SignalSyntax._
import zio.clock.Clock
import zio.duration._
import zio._
import zio.random.Random

case class VisualTask[R, A](f: URIO[R, A]) extends Owner {
  private val result      = Var(Option.empty[A])
  private val $result     = result.signal
  private val $isComplete = $result.map(_.isDefined)

  private val isRunning  = Var(false)
  private val $isRunning = isRunning.signal

  private val isInterrupted  = Var(false)
  private val $isInterrupted = isInterrupted.signal

  private val progress  = Var(0.0)
  private val $progress = progress.signal

  private def updateProgress(duration: Duration): UIO[Unit] =
    UIO {
      from(0.0)
        .to(1.0, duration.toMillis.toInt, easing = Some(Easing.cubic.inOut))
        .run
        .addObserver(progress.writer)(this)
    }

  def runRandom(from: Duration = 700.millis, to: Duration = 2.seconds): ZIO[R with Random with Clock, Nothing, A] =
    random.nextLongBetween(from.toNanos, to.toNanos).map(Duration.fromNanos).flatMap(run)

  def reset =
    UIO {
      killSubscriptions()
      isRunning.set(false)
      isInterrupted.set(false)
      result.set(None)
      progress.set(0.0)
    }

  def run(duration: Duration = 1.second): ZIO[R with Clock, Nothing, A] =
    (for {
      _ <- reset
      _ <- UIO(isRunning.set(true))
      _ <- updateProgress(duration).delay(300.millis)
      _ <- ZIO.sleep(duration)
      a <- f
      _ <- UIO(result.set(Some(a)))
    } yield a)
      .ensuring(UIO(isRunning.set(false)))
      .onInterrupt(UIO(isInterrupted.set(true)))

  def render =
    div(
      width := "46px",
      height := "46px",
      textAlign.center,
      overflow.hidden,
      marginRight := "12px",
      borderRadius := "4px",
      background <-- $background,
      position.relative,
      div(
        position.absolute,
        top := "0",
        height := "100%",
        opacity <-- $isRunning.combineWith($isComplete).map(b => if (b._1 || b._2) 1.0 else 0.0).spring,
        width <-- $progress.map(_ * 46).px,
        background <-- $isComplete.map(b => if (b) (220.0, 80.0) else (80.0, 255.0)).spring.map {
          case (g, b) => s"rgba(80,$g,$b,0.8)"
        }
      ),
      div(
        position.relative,
        zIndex := "10",
        padding := "8px",
        textAlign.center,
        child.text <-- $result.combineWith($isInterrupted)
          .combineWith($isRunning)
          .map {
            case ((result, interrupted), running) =>
              if (interrupted) "⚠️️"
              else if (running) ""
              else
                result match {
                  case Some(value) => value.toString
                  case None        => "_"
                }
          }
      ),
    )

  private val $background = spring($isRunning.combineWith($isInterrupted).map {
    case (running, interrupted) =>
      val alpha = if (running) 0.8 else 0.4
      if (interrupted)
        (160.0, 120.0, 80.0, alpha)
      else
        (80.0, 80.0, 160.0, alpha)
  }).map { case (r, g, b, a) => s"rgba($r,$g,$b,$a)" }

}

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
