package formula

import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._
import zio._
import zio.clock.Clock
import zio.duration.durationInt

//trait Executable[I, S, O] {
// current observable value
//  def call(input: I): UIO[O]
//}

object ExamplesExec {
  def main(args: Array[String]): Unit = {
    Find(4, List(3, 4, 5))
  }
}

trait Executable[Input, State, Output] {
  implicit val runner: Runner[Input, Output, State]
  def make(input: Input): State

  val variable                       = Var(Option.empty[State])
  def $signal: Signal[Option[State]] = variable.signal

  def apply(input: Input): URIO[Clock, Output] = {
    variable.set(Some(make(input)))
    step
  }

  def step: URIO[Clock, Output] = {
    runner.step(variable.now().get) match {
      case Left(next) =>
        zio.clock.sleep(500.millis) *>
          UIO(variable.set(Some(next))) *>
          step
      case Right(result) =>
        zio.clock.sleep(1.second) *> ZIO.succeed(result)
    }
  }

}

trait Runnable[Input, Output, Self] {
  def step: Either[Self, Output]
}

trait Runner[Input, Output, A] { self =>
  def step(value: A): Either[A, Output]
  def make(input: Input): A

  private def make0(input: Input) = make(input)

  def makeExecutable: Executable[Input, A, Output] = new Executable[Input, A, Output] {
    override implicit val runner: Runner[Input, Output, A] = self

    override def make(input: Input): A = make0(input)
  }
}

object Runner {
  implicit final class RunnerOps[A](val value: A) extends AnyVal {
    def step[I, O](implicit runner: Runner[I, O, A]): Either[A, O] = runner.step(value)
  }
}

case class Find(target: Int, list: List[Int], current: Int = 0) extends Runnable[(Int, List[Int]), Option[Int], Find] {
  def step: Either[Find, Option[Int]] =
    if (current >= list.length) {
      Right(None)
    } else if (list(current) == target) {
      Right(Some(current))
    } else {
      Left(copy(current = current + 1))
    }
}

object Find {
  implicit val runner: Runner[(Int, List[Int]), Option[Int], Find] = new Runner[(Int, List[Int]), Option[Int], Find] {
    override def step(value: Find): Either[Find, Option[Int]] = value.step
    override def make(input: (Int, List[Int])): Find          = Find(input._1, input._2)
  }
}
