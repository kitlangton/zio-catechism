package formula

import com.raquo.laminar.api.L._

case class LensVar[A](signal: Signal[A], writer: Observer[A], now: () => A) {
  def lens[B](from: A => B)(to: (A, B) => A): LensVar[B] = {
    val signal2: Signal[B]   = signal.map(from)
    val writer2: Observer[B] = writer.contramap[B](to(now(), _))
    LensVar(signal2, writer2, () => from(now()))
  }

}

object LensVar {
  def fromVar[A](variable: Var[A]): LensVar[A] = LensVar(variable.signal, variable.writer, () => variable.now())

  implicit final class VarOps[A](val variable: Var[A]) extends AnyVal {
    def lens: LensVar[A]                                   = LensVar.fromVar(variable)
    def lens[B](from: A => B)(to: (A, B) => A): LensVar[B] = lens.lens(from)(to)
  }
}
