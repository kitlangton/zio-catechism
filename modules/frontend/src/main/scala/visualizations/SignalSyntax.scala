package visualizations

import animator.{Animatable, Animator}
import com.raquo.airstream.signal.Signal

object SignalSyntax {
  implicit final class SignalOps[A](val value: Signal[A]) extends AnyVal {
    def string: Signal[String]                                = value.map(_.toString)
    def spring(implicit animatable: Animatable[A]): Signal[A] = Animator.spring(value)
  }

  implicit final class NumericSignalOps[A: Numeric](value: Signal[A]) {
    def px(implicit numeric: Numeric[A]): Signal[String] = value.map(d => s"${d}px")
  }

  implicit final class BooleanSignalOps(val value: Signal[Boolean]) extends AnyVal {
    def percent: Signal[Double] = value.map(b => if (b) 1.0 else 0.0).spring
  }
}
