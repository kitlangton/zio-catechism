package catechism

import animator.Animator.{RAFStream, spring}
import animator.{Animation, Color}
import blogus.components.{codeBlock, inlineCode}
import catechism.ObservableSyntax.NumericSignalOps
import catechism.ZioCatechism.LessonError
import catechism.ZioSyntax.ZioOps
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import zio.Schedule.Decision
import zio._
import zio.duration.durationInt

import scala.math.Ordered.orderingToOrdered

object Timeline {

  lazy val deltaStream: EventStream[Double] = {
    var last = 0.0
    RAFStream.map { a =>
      val result =
        if (last == 0) 0
        else a - last
      last = a
      result
    }
  }

  case class ScheduleLesson(title: String, schedule: Schedule[ZEnv, Any, Any], codeString: String)

  val scheduleLessons: Seq[ScheduleLesson] = List(
    ScheduleLesson(".spaced", Schedule.spaced(1.second), "Schedule.spaced(1.second)"),
    ScheduleLesson(".recurs",
                   Schedule.spaced(1.second) && Schedule.recurs(3),
                   "Schedule.spaced(1.second) && Schedule.recurs(3)"),
    ScheduleLesson(".exponential", Schedule.exponential(300.millis), "Schedule.exponential(300.millis)"),
    ScheduleLesson(
      ".elapsed",
      Schedule.spaced(1.second) && Schedule.elapsed.whileOutput(dur => dur < 5.seconds),
      "Schedule.spaced(1.second) && Schedule.elapsed.whileOutput(_ < 5.seconds)"
    )
  )

  lazy val body = div(
    scheduleLessons.map(run).flatMap(a => Seq(a, hr(opacity := "0.2"))).dropRight(1)
  )

  def run(lesson: ScheduleLesson): ReactiveHtmlElement[html.Div] = {
    val isRunning   = Var(false)
    lazy val runLog = Var(Timer.empty)

    lazy val zeffect =
      ZIO
        .uninterruptibleMask { restore =>
          for {
            _ <- UIO(runLog.set(Timer.empty))
            _ <- UIO(isRunning.set(true))
            r <- (for {
              _ <- UIO(runLog.update(_.next(ExecutionStatus.Running)))
              _ <- ZIO.sleep(500.millis)
              _ <- restore(ZIO.fail(LessonError.RandomFailure))
            } yield ())
              .retry(lesson.schedule.onDecision {
                case _: Decision.Done[_, _] =>
                  UIO.unit
                case _: Decision.Continue[_, _, _] =>
                  UIO(runLog.update(_.next(ExecutionStatus.Suspended)))
              })
              .ensuring(UIO(isRunning.set(false)))
          } yield r
        }
        .timeout(15.seconds)

    val hashId = s"Schedule${lesson.title}"

    div(
      idAttr := hashId,
      a(
        href := s"#$hashId",
        inlineCode(lesson.title),
      ),
      display.flex,
      flexDirection.column,
      deltaStream.filter(_ => isRunning.now()) --> { ms =>
        runLog.update(_ + ms)
      },
      justifyContent.center,
      margin := "12px 0",
      div(
        margin := "12px 0",
        padding := "32px 8px 12px 8px",
        borderRadius := "4px",
        border := "1px solid #223",
        background := "#112",
        renderTimer(runLog.signal),
      ),
      div(
        cls <-- isRunning.signal.map(b => if (b) "main running" else "main"),
        codeBlock(lesson.codeString),
        cursor.pointer,
        onClick --> { _ =>
          if (!isRunning.now()) zeffect.runAsync
        }
      ),
    )
  }

  case class Timer(current: Timed, history: Seq[Timed] = Seq.empty) { self =>
    def +(millis: Double): Timer = self + millis.toInt
    def +(millis: Int): Timer    = copy(current = current.copy(duration = current.duration + millis))

    def next(executionStatus: ExecutionStatus): Timer =
      copy(current = Timed(executionStatus, 0), history = history.appended(current))

    def all: Seq[Timed] = history :+ current
  }

  object Timer {
    val empty: Timer = Timer(Timed(ExecutionStatus.Running, 0))
  }

  case class Timed(executionStatus: ExecutionStatus, duration: Int)

  sealed trait ExecutionStatus
  object ExecutionStatus {
    case object Suspended extends ExecutionStatus
    case object Running   extends ExecutionStatus
  }

  private def renderTimer($timer: Signal[Timer]) = div(
    div(
      display.flex,
      marginTop := "8px",
      borderRadius := "2px",
      alignItems.center,
      children <-- $timer.map(_.all.zipWithIndex).split(_._2) { (_, _, $value) =>
        renderTimed($value.map(_._1))
      },
    )
  )

  def round(double: Double, v: Int): Double = Math.round(double / v) * v

  private def renderTimed($timed: Signal[Timed]) = {
    val $executionStatus = $timed.map(_.executionStatus)
    val niceBlue         = Color(80, 80, 255)
    div(
      position.relative,
      display.flex,
      justifyContent.center,
      height := "8px",
      borderRadius := "8px",
      width <-- spring($timed.map { _.duration / 10.0 }).px,
      background <-- $executionStatus.flatMap {
        case ExecutionStatus.Suspended =>
          Animation
            .from(niceBlue)
            .to(Color(120, 120, 120))
            .to(Color(120, 120, 120).copy(alpha = 0.7), 10000)
            .run
        case ExecutionStatus.Running =>
          Animation
            .from(Color(180, 180, 255))
            .to(niceBlue, 500)
            .to(niceBlue.copy(alpha = 0.8), delay = 200)
            .to(niceBlue.copy(alpha = 0.6), 10000)
            .run
      }.map(_.css),
      div(
        textAlign.center,
        top := "-30px",
        opacity <-- $executionStatus.flatMap {
          case ExecutionStatus.Suspended => Animation.from(0.0).to(1.0).run
          case ExecutionStatus.Running   => Val(0.0)
        },
        position.absolute,
        fontSize := "0.8em",
        padding := "0px 4px",
        child <-- $timed.map(timed => f"${round(timed.duration, 100) / 1000.0}%1.1fs"),
        background := "#334",
        borderRadius := "4px",
      )
    )
  }

  lazy val line = div(
    position.absolute,
    background := "#444",
    height := "1px",
    width := "100%",
  )

}
