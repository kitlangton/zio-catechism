package catechism

import animator.{Animatable, Animator}
import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._

object ObservableSyntax {
  implicit final class SignalOps[A](val value: Signal[A]) extends AnyVal {
    def spring(implicit animatable: Animatable[A]): Signal[A] = Animator.spring(value)
  }

  implicit final class BooleanSignalOps(val value: Signal[Boolean]) extends AnyVal {
    def percent: Signal[Double] = value.map(b => if (b) 1.0 else 0.0).spring
  }

  implicit final class ObservableOps[A](val value: Observable[A]) extends AnyVal {
    def string: Observable[String] = value.map(_.toString)
  }

  implicit final class NumericSignalOps[A: Numeric](value: Observable[A]) {
    def px(implicit numeric: Numeric[A]): Observable[String] = value.map(d => s"${d}px")
  }

}
