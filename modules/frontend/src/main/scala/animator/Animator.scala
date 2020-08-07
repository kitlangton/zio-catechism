package animator

import animator.AnimationStatus.{Animating, Done}
import animator.Tween._
import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Var}
import magnolia.{CaseClass, Magnolia}
import org.scalajs.dom

import scala.collection.mutable
import scala.language.experimental.macros
import scala.util.Success

object Animator {

  trait Animatable[A] {
    def size: Int

    def toAnimations(value: A): mutable.IndexedSeq[Double]

    def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): A
  }

  // Magnolia derivation for Case Classes
  object Animatable extends AnimatableInstances {
    type Typeclass[T] = Animatable[T]

    def combine[T](ctx: CaseClass[Animatable, T]): Animatable[T] =
      new Animatable[T] {
        override def size: Int = ctx.parameters.length

        override def toAnimations(value: T): mutable.IndexedSeq[Double] = {
          val seq = mutable.IndexedSeq.newBuilder[Double]
          ctx.parameters.foreach { param =>
            seq.addAll(
              param.typeclass.toAnimations(param.dereference(value))
            )
          }
          seq.result()
        }

        override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): T = {
          var index = 0
          ctx.construct { param =>
            val animatable = param.typeclass
            val value      = animatable.fromAnimations(animations.slice(index, animatable.size))
            index += animatable.size
            value
          }
        }
      }

    implicit def gen[T]: Animatable[T] = macro Magnolia.gen[T]
  }

  def signalFromValue[A](value: A): Signal[A] =
    EventStream.fromValue(value, true).startWith(value)

  def animate[A](from: A, to: A, delay: Int = 0, duration: Int = 600, easing: Easing = Easing.sine.inOut)(
      implicit animatable: Animatable[A]): Signal[AnimationStatus[A]] = {
    val runner = new Runner[A] {}
    runner.animateTo(from, to, delay = delay, duration = duration, easing = easing)
  }

  class Runner[A](implicit animatable: Animatable[A]) {
    var values: mutable.IndexedSeq[Eased] = mutable.IndexedSeq.empty[Eased]
    var animating                         = Var(true)
    val time = animating.signal.map {
      case true  => RAFStream
      case false => EventStream.empty
    }.flatten

    def animateTo(from: A, to: A, delay: Int, easing: Easing, duration: Int): Signal[AnimationStatus[A]] = {
      val signal = time
        .map { t =>
          if (values.isEmpty) {
            values = animatable
              .toAnimations(from)
              .map(
                value =>
                  Eased
                    .fromValue(value, t)
                    .copy(delay = delay, easing = easing, duration = duration))
            val toValues = animatable.toAnimations(to)
            var i        = -1
            values.foreach { tween =>
              i += 1
              tween.setTarget(toValues(i))
            }
          }

          values.foreach(_.tick(t))
          if (values.forall(_.isDone)) {
            animating.set(false)
            Done(animatable.fromAnimations(values))
          } else {
            Animating(animatable.fromAnimations(values))
          }
        }
        .startWith(Animating(from))

      if (!animating.now()) {
        animating.set(true)
      }

      signal
    }
  }

  def spring[A]($value: Signal[A],
                stiffness: Double = 170,
                damping: Double = 26,
                delay: Int = 0,
                delaySignal: Option[Signal[Int]] = None,
                startFrom: Option[A] = None)(implicit animatable: Animatable[A]): Signal[A] = {
    val delayedValue = delaySignal match {
      case Some(sig) => sig.flatMap(d => $value.composeChanges(_.delay(d)))
      case None =>
        if (delay > 0) $value.composeChanges(_.delay(delay.toInt)) else $value
    }
    val runner = new SpringRunner[A] {}

    delayedValue
      .flatMap(runner.animateTo(_, stiffness = stiffness, damping = damping, startFrom = startFrom))
  }

  object RAFStream extends EventStream[Double] {
    override val topoRank: Int = 1

    var started           = false
    var lastValue: Double = 0

    def tick(): Int =
      dom.window.requestAnimationFrame(step)

    def step(t: Double): Unit = {
      lastValue = t
      new Transaction(fireTry(Success(t), _))
      if (started) tick()
    }

    override protected[this] def onStart(): Unit = {
      started = true
      tick()
    }

    override protected[this] def onStop(): Unit = {
      started = false
    }
  }

  class SpringRunner[A](implicit animatable: Animatable[A]) {
    var values: mutable.IndexedSeq[Spring] = mutable.IndexedSeq.empty[Spring]
    var animating                          = Var(true)
    val time = animating.signal.flatMap {
      case true  => RAFStream
      case false => EventStream.empty
    }

    def animateTo(value: A, stiffness: Double = 170, damping: Double = 26, startFrom: Option[A] = None): Signal[A] = {
      if (values.isEmpty) {
        values = animatable
          .toAnimations(value)
          .map(
            d =>
              Spring
                .fromValue(d, 0)
                .copy(stiffness = stiffness, damping = damping))
        startFrom.foreach { startValue =>
          val nextValues = animatable.toAnimations(startValue)
          var i          = -1
          values.foreach { spring =>
            i += 1
            spring.setTarget(nextValues(i))
          }
        }
      } else {
        val nextValues = animatable.toAnimations(value)
        var i          = -1
        values.foreach { spring =>
          i += 1
          spring.setTarget(nextValues(i))
        }
      }

      val signal = time
        .map { t =>
          values.foreach(_.tick(t))
          if (values.forall(_.isDone)) {
            animating.set(false)
          }
          animatable.fromAnimations(values)
        }
        .startWith(animatable.fromAnimations(values))

      if (!animating.now()) {
        animating.set(true)
      }

      signal
    }
  }
}

// Notes
//  from,    to
//  def tween(from: A, to: A, interpolator: (Double, Double) => Double): A
//
// Traverse
// LAWS
// interpolator == ._1 then Coord is from
// interpolator == ._2 then Coord is to
// tween(A,A) == every pair of doubles will be the same
//
// Coord(0,10) --> Coord(5,20)
//
// interpolator may be a lambda
//  def coordTween(from: Coord, to: Coord, interpolator: (Double, Double) => Double): Coord = {
//    Coord(
//      x = interpolator(from.x, to.x),
//      y = interpolator(from.y, to.y),
//    )
//  }
