package catechism

import com.raquo.laminar.api.L._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle
import TransitionStatus._
import catechism.ObservableSyntax._
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

sealed trait TransitionStatus
object TransitionStatus {
  case object Inserting extends TransitionStatus
  case object Active    extends TransitionStatus
  case object Removing  extends TransitionStatus
}

object Transitions {
  def transitioning[A, Key, Output]($as: Signal[A])(key: A => Key)(
      project: (Key, A, Signal[A], Signal[TransitionStatus]) => Output): Signal[Seq[Output]] = {
    splitTransition($as.map(Seq(_)))(key)(project)
  }

  def splitTransition[A, Key, Output]($as: Signal[Seq[A]])(key: A => Key)(
      project: (Key, A, Signal[A], Signal[TransitionStatus]) => Output): Signal[Seq[Output]] = {
    type StatusList = Seq[(A, TransitionStatus)]

    val timerMap = mutable.Map.empty[Key, SetTimeoutHandle]

    val $activationBus = new EventBus[Key]
    val $removeBus     = new EventBus[Key]

    val $activate = $activationBus.events.map { key0 => m: StatusList =>
      m.map {
        case (a, _) if key(a) == key0 => (a, Active)
        case other                    => other
      }
    }

    val $remove = $removeBus.events.map { key0 => m: StatusList =>
      m.filter {
        case (a, _) if key(a) == key0 => false
        case _                        => true
      }
    }

    def cancelTimer(key: Key): Unit =
      timerMap.get(key).foreach(handle => js.timers.clearTimeout(handle))

    def addTimer(key: Key, ms: Int = 0)(body: => Unit): Unit = {
      cancelTimer(key)
      timerMap(key) = js.timers.setTimeout(ms)(body)
    }

    val $adding = $as.map { as => m: StatusList =>
      val newAs: StatusList = as
        .filterNot(a => m.exists(v => key(v._1) == key(a)))
        .map(_ -> Inserting)

      val updatedAs: StatusList = as
        .filter(a => m.exists(v => key(v._1) == key(a)))
        .map(_ -> Active)

      val removedAs: StatusList = m
        .map {
          case a if !as.exists(v => key(v) == key(a._1)) =>
            addTimer(key(a._1), 700) {
              $removeBus.writer.onNext(key(a._1))
            }
            a._1 -> Removing
          case (a, Removing) =>
            cancelTimer(key(a))
            a -> Active
          case other => other
        }

      newAs.foreach {
        case (a, _) =>
          addTimer(key(a)) {
            $activationBus.writer.onNext(key(a))
          }
      }

      removedAs ++ newAs ++ updatedAs
    }

    val changes = EventStream
      .merge[StatusList => StatusList]($activate, $adding.changes, $remove)

    val $statuses: Signal[StatusList] =
      changes.fold(Seq.empty[(A, TransitionStatus)]) { case (sm, f) => f(sm) }

    $statuses.split(v => key(v._1)) { (k, init, $a) =>
      project(k, init._1, $a.map(_._1), $a.map(_._2))
    }
  }

  def splitOption[A, Output]($as: Signal[Option[A]])(
      project: (A, Signal[A], Signal[TransitionStatus]) => Output): Signal[Option[Output]] = {
    val timers = mutable.Map.empty[Int, SetTimeoutHandle]

    var lastValue = Option.empty[A]

    val removalBus: EventBus[Option[(A, TransitionStatus)]] =
      new EventBus[Option[(A, TransitionStatus)]]()

    def cancelTimer(timerId: Int): Unit =
      timers.get(timerId).foreach(handle => js.timers.clearTimeout(handle))

    def addTimer(timerId: Int, ms: Int = 0)(body: => Unit): Unit = {
      cancelTimer(timerId)
      timers(timerId) = js.timers.setTimeout(ms)(body)
    }

    val changeMap: Signal[Option[(A, TransitionStatus)]] =
      $as.map {
        case Some(value) =>
          cancelTimer(1)
          lastValue match {
            case Some(_) =>
              lastValue = Some(value)
              Some((value, Active))
            case None =>
              lastValue = Some(value)
              addTimer(0, 0) {
                removalBus.writer.onNext(lastValue.map(_ -> Active))
              }
              Some((value, Inserting))
          }
        case None =>
          lastValue match {
            case Some(value) =>
              addTimer(1, 700) {
                lastValue = None
                removalBus.writer.onNext(None)
              }
              Some((value, Removing))
            case None =>
              None
          }
      }

    val $events = changeMap
      .composeChanges(stream => EventStream.merge(stream, removalBus.events))
      .debugLog("HELP")

    $events.split(_ => 1) { (k, init, $a) =>
      project(init._1, $a.map(_._1), $a.map(_._2))
    }
  }

  def slide[A, Key](signal: Signal[Seq[A]])(key: A => Key)(
      render: (A, Signal[A]) => ReactiveHtmlElement.Base): ReactiveHtmlElement[html.Div] =
    div(
      children <-- Transitions.splitTransition(signal)(key) { (_, a, $a, $status) =>
        val $opacity = $status.map {
          case TransitionStatus.Active => 1.0
          case _                       => 0.0
        }.spring

//      val $position = $status.map {
//        case TransitionStatus.Active => "relative"
//        case _                       => "absolute"
//      }

        div(
          opacity <-- $opacity,
//        position <-- $position,
          overflowY.hidden,
          child.text <-- $status.string,
          width := "650px",
          margin := "0",
          padding := "0",
          render(a, $a),
          onMountBind { ref =>
            maxHeight <-- $status.map {
              case TransitionStatus.Active => ref.thisNode.ref.scrollHeight.toDouble
              case _                       => 0.0
            }.spring.px
          },
        )
      }
    )

  def slide(signal: Signal[Option[ReactiveHtmlElement.Base]]): Signal[Option[ReactiveHtmlElement.Base]] =
    Transitions.splitOption(signal) { (_, $a, $status) =>
      val $finished = $status.map {
        case TransitionStatus.Active => 1.0
        case _                       => 0.0
      }.spring

      div(
        overflowY.hidden,
        inContext { el =>
          Seq(
            maxHeight <-- $finished.map {
              _ * el.ref.scrollHeight.toDouble
            }.px,
            opacity <-- $finished
          )
        },
        child <-- $a
      )
    }

}
