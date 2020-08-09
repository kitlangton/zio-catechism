package catechism

import animator.Animation._
import animator._
import animator.Animator.spring
import com.raquo.laminar.api.L.{Ref => _, _}
import com.raquo.laminar.ext.CSS.fontVariant
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{ClientRect, EventSource, html, window}
import catechism.SignalSyntax._
import zio.clock.Clock
import zio.duration._
import zio._
import zio.random.Random

trait Renderable {
  def render: ReactiveHtmlElement.Base
  def reset: UIO[Unit]
}

case class VisualTask[R, E, A](f: ZIO[R, E, A]) extends Owner with Renderable {
  private val result      = Var(Option.empty[A])
  private val $result     = result.signal
  private val $isComplete = $result.map(_.isDefined)

  private val isRunning  = Var(false)
  private val $isRunning = isRunning.signal

  private val $justCompleted =
    EventStream
      .merge($isComplete.changes.filter(a => a).mapTo(1.0), $isComplete.changes.filter(a => a).delay(200).mapTo(0.0))
      .toSignal(0.0)
      .spring

  private val isInterrupted  = Var(false)
  private val $isInterrupted = isInterrupted.signal

  private val error  = Var(Option.empty[E])
  private val $error = error.signal

  private val $justInterrupted =
    EventStream
      .merge($isInterrupted.changes.filter(a => a).mapTo(1.0),
             $isInterrupted.changes.filter(a => a).delay(200).mapTo(0.0))
      .toSignal(0.0)
      .spring

  private val progress  = Var(0.0)
  private val $progress = progress.signal

  private def updateProgress(duration: Duration): UIO[Unit] =
    UIO {
      from(0.0)
        .to(1.0, duration.toMillis.toInt, easing = Some(Easing.cubic.inOut))
        .run
        .addObserver(progress.writer)(this)
    }

  def runSlow: ZIO[R with Random with Clock, E, A] =
    runRandom(800.millis, 2.seconds)

  def runRandom(from: Duration = 300.millis, to: Duration = 700.millis): ZIO[R with Random with Clock, E, A] =
    random.nextLongBetween(from.toNanos, to.toNanos).map(Duration.fromNanos).flatMap(run)

  def reset: UIO[Unit] =
    UIO {
      killSubscriptions()
      error.set(None)
      isRunning.set(false)
      isInterrupted.set(false)
      result.set(None)
      progress.set(0.0)
    }

  def run(duration: Duration = 1.second): ZIO[R with Clock, E, A] =
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
      .tapError(e => UIO(error.set(Some(e))))

  def render =
    div(
      width := "46px",
      height <-- $isRunning.percent.map(p => 46.0 - (p * 30.0)).px,
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
        background <-- $isComplete.map(b => if (b) (0.8, 160.0) else (0.9, 255.0)).spring.map {
          case (a, b) => s"rgba(80,80,$b,$a)"
        }
      ),
      div(
        position.relative,
        zIndex := "10",
        padding := "8px",
        textAlign.center,
        div(
          opacity <-- $isRunning.combineWith($isComplete).map {
            case (true, _) => 0.0
            case (b1, b2)  => if (!(b1 || b2)) 0.4 else 1.0
          }.spring,
          child.text <-- $result.combineWith($isInterrupted)
            .combineWith($isRunning)
            .combineWith($error)
            .map {
              case (((result, interrupted), running), error) =>
                if (error.isDefined) "☠️"
                else if (interrupted) "⚠️️"
                else if (running) "✦"
                else
                  result match {
                    case Some(())    => "👍"
                    case Some(value) => value.toString
                    case None        => "✦"
                  }
            }
        ),
      ),
      div(
        position.absolute,
        top := "0",
        zIndex := "30",
        opacity <-- $justCompleted,
        background := "rgba(255,255,255,0.3)",
        height := "46px",
        width := "46px",
      ),
      div(
        position.absolute,
        top := "0",
        zIndex := "30",
        opacity <-- $justInterrupted,
        background := "rgba(180,180,60,0.3)",
        height := "46px",
        width := "46px",
      ),
    )

  private val $background = spring($isRunning.combineWith($isInterrupted).combineWith($error).map {
    case ((running, interrupted), error) =>
      val alpha = if (running) 0.8 else 0.4
      if (error.isDefined)
        (180.0, 80.0, 80.0, alpha)
      else if (interrupted)
        (160.0, 120.0, 80.0, alpha)
      else
        (80.0, 80.0, 160.0, alpha)
  }).map { case (r, g, b, a) => s"rgba($r,$g,$b,$a)" }

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
      borderRadius := "4px",
      background <-- $background,
      display.flex,
      overflow.hidden,
      div(
        child.text <-- $text
      ),
      onMountBind { el: MountContext[ReactiveHtmlElement.Base] =>
        width <-- $text.mapTo(el.thisNode.ref.firstElementChild.scrollWidth.toDouble + 16).spring.px
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
    case ((updating, interrupted), error) =>
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
