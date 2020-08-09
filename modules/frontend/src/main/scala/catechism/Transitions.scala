package catechism

import com.raquo.laminar.api.L._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle
import TransitionStatus._

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
        .map(a => (a, Inserting))

      val removedAs: StatusList = m
        .map {
          case a if !as.exists(v => key(v) == key(a._1)) =>
            addTimer(key(a._1), 700) {
              $removeBus.writer.onNext(key(a._1))
            }
            (a._1 -> Removing)
          case (a, Removing) =>
            cancelTimer(key(a))
            (a, Active)
          case other => other
        }

      newAs.foreach {
        case (a, _) =>
          addTimer(key(a)) {
            $activationBus.writer.onNext(key(a))
          }
      }

      removedAs ++ newAs
    }

    val changes = EventStream.merge[StatusList => StatusList]($activate, $adding.changes, $remove)

    val $statuses: Signal[StatusList] = changes.fold(Seq.empty[(A, TransitionStatus)]) { case (sm, f) => f(sm) }

    $statuses.split(v => key(v._1)) { (k, init, $a) =>
      project(k, init._1, $a.map(_._1), $a.map(_._2))
    }
  }
}
