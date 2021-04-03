package catechism

import animator.Animation.from
import animator.Animator.spring
import animator.Easing
import catechism.ObservableSyntax._
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import zio.clock.Clock
import zio.duration._
import zio.random.Random
import zio._

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

  def render: ReactiveHtmlElement.Base =
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
              case (result, interrupted, running, error) =>
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
    case (running, interrupted, error) =>
      val alpha = if (running) 0.8 else 0.4
      if (error.isDefined)
        (180.0, 80.0, 80.0, alpha)
      else if (interrupted)
        (160.0, 120.0, 80.0, alpha)
      else
        (80.0, 80.0, 160.0, alpha)
  }).map { case (r, g, b, a) => s"rgba($r,$g,$b,$a)" }

}
