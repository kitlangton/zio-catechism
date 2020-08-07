package visualizations

import com.raquo.airstream.signal.Signal

object SignalSyntax {
  implicit final class SignalOps[A](val value: Signal[A]) extends AnyVal {
    def string: Signal[String]                           = value.map(_.toString)
    def px(implicit numeric: Numeric[A]): Signal[String] = value.map(d => s"${d}px")
  }
}
