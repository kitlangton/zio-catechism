package animator

import AnimationStatus.{Animating, Done}
import Animation._
import Animator._
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.core.Signal

object Animation {
  def from[A: Animatable](value: => A): Constant[A] = Constant(value)

  implicit def easingToOption(value: Easing): Option[Easing] = Some(value)

  case class Constant[A: Animatable](value: A) extends Animation[A] { self =>
    def startValue: A = value
    override def __run: Signal[AnimationStatus[A]] =
      signalFromValue(Done(value))

    override def factor(amount: Double): Animation[A] = self
  }

  case class Map[A: Animatable, B: Animatable](animation: Animation[A], f: A => B) extends Animation[B] {
    override private[animator] def startValue: B = f(animation.startValue)

    override private[animator] def __run: Signal[AnimationStatus[B]] =
      animation.__run.map(_.map(f))

    override def factor(double: Double): Animation[B] =
      animation.factor(double).map(f)
  }

  case class Hold[A: Animatable](animation: Animation[A], delayMillis: Int) extends Animation[A] { self =>
    def startValue: A = animation.startValue

    override def __run: Signal[AnimationStatus[A]] = {
      val $run = animation.__run
      $run.flatMap {
        case _: Animating[A] => $run
        case Done(value) =>
          EventStream
            .fromValue(Done(value), emitOnce = true)
            .delay(delayMillis)
            .startWith(Animating(value))
      }
    }

    override def factor(amount: Double): Animation[A] =
      Hold(animation.factor(amount), (delayMillis * amount).toInt)
  }

  case class Springing[A: Animatable](signal: Signal[A]) extends Animation[A] with Owner { self =>
    override private[animator] def startValue = signal.observe(self).now()

    override private[animator] def __run: Signal[AnimationStatus[A]] =
      spring(signal).map(Animating(_))

    override def factor(double: Double): Animation[A] = self
  }

  case class Forever[A: Animatable](animation: Animation[A]) extends Animation[A] { self =>
    override private[animator] def startValue = animation.startValue

    override private[animator] def __run: Signal[AnimationStatus[A]] = {
      val $animation = animation.__run

      $animation.flatMap {
        case _: Animating[A] => $animation
        case _: Done[A] =>
          self.__run
      }
    }

    override def factor(double: Double): Animation[A] = self
  }

  case class Sequence[A: Animatable](lhs: Animation[A],
                                     rhs: Animation[A],
                                     easing: Option[Easing] = None,
                                     duration: Int = 600,
                                     delay: Int = 0)
      extends Animation[A] { self =>
    def startValue: A = lhs.startValue

    override def __run: Signal[AnimationStatus[A]] = {
      val $lhs = lhs.__run

      $lhs.flatMap {
        case _: Animating[A] => $lhs
        case Done(value) =>
          val $animate = animate(value,
                                 rhs.startValue,
                                 delay = delay,
                                 duration = duration,
                                 easing = easing.getOrElse(Easing.sine.inOut))
          $animate.flatMap {
            case _: Animating[A] => $animate
            case Done(_) =>
              rhs.__run
          }
      }
    }

    def wait(durationMs: Int): Animation[A] = copy(rhs = rhs.wait(durationMs))

    override def reverse: Animation[A] =
      copy(lhs = rhs.reverse, rhs = lhs.reverse)

    def dropFirst: Animation[A] = {
      lhs match {
        case lhs0: Sequence[A] => copy(lhs = lhs0.dropFirst)
        case _                 => rhs
      }
    }

    def andBack: Animation[A] =
      self >> (lhs match {
        case sequence: Sequence[A] => sequence.dropFirst.reverse
        case _                     => lhs.reverse
      })

    override def factor(amount: Double): Animation[A] =
      copy(lhs = lhs.factor(amount), rhs = rhs.factor(amount))
  }
}

sealed trait Animation[A] { self =>
  private[animator] def startValue: A

  private[animator] def __run: Signal[AnimationStatus[A]]

  def run: Signal[A] = __run.map(_.value)

  def wait(durationMs: Int)(implicit animatable: Animatable[A]): Animation[A] =
    Sequence(Constant(startValue), self, duration = durationMs)

  def factor(double: Double): Animation[A]

  def reverse: Animation[A] = self

  def loop(implicit animatable: Animatable[A]): Animation[A] =
    self >> self >> self >> self

  def >>(rhs: => Animation[A])(implicit animatable: Animatable[A]): Animation[A] =
    Animation.Sequence(self, rhs)

  def forever(implicit animatable: Animatable[A]): Animation[A] =
    Forever(self)

  def andBack(implicit animatable: Animatable[A]): Animation[A] =
    self >> self.reverse

  def to(rhs: Animation[A])(implicit animatable: Animatable[A]): Sequence[A] =
    Sequence(self, rhs)

  def to(rhs: A, duration: Int = 600, delay: Int = 0, easing: Option[Easing] = None)(
      implicit animatable: Animatable[A]): Sequence[A] =
    Sequence(self, Constant(rhs), delay = delay, duration = duration, easing = easing)

  def map[B: Animatable](f: A => B)(implicit animatable: Animatable[A]): Animation[B] =
    Animation.Map(self, f)
}
