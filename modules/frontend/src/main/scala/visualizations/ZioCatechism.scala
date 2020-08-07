package visualizations

import animator.Animator.spring
import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import com.raquo.laminar.api.L._
import com.raquo.laminar.ext.CSS.fontVariant
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import visualizations.ZioSyntax.ZioOps
import zio._
import zio.clock.Clock
import zio.duration._
import zio.random.Random

object ZioCatechism {
  private val delay = 100.millis
  private val _     = UIO(()).runAsync

  private val header = div(
    div(
      "ZIO",
      fontWeight := "bold",
      fontSize := "2rem",
      marginBottom := "-12px"
    ),
    div(
      "see-oh",
      opacity := "0.7",
      fontSize := "1.4rem",
    ),
    textAlign.center,
    fontVariant.smallCaps,
    marginBottom := "30px"
  )

  lazy val main = div(
    maxWidth := "700px",
    margin := "20px auto 0 auto",
    header,
    md"### Iteration",
    foreach,
    hr(opacity := "0.2"),
    foreachPar,
    hr(opacity := "0.2"),
    md"""
#### Underscore Variants

When a ZIO method name ends in `_` *(i.e., `foreach_`)*, the result is discarded, returning `Unit`.
""",
    foreach_,
    hr(opacity := "0.2"),
    foreachPar_,
    hr(opacity := "0.2"),
    md"### Forking",
    forkTwice,
    hr(opacity := "0.2"),
    md"### Racing",
    race,
  )

  lazy val foreach = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val n4      = ZVar(25)
    val n5      = ZVar(30)
    val sum     = ZVar.result(Option.empty[Int])
    val numbers = List(n1, n2, n3, n4, n5)
    val running = Var(false)

    def addFive(number: ZVar[Int]) = number.update(_ + 5)

    val addFiveToAll: URIO[Clock, Unit] = ZIO
      .foreach(numbers) { n =>
        addFive(n) *> UIO(n.now)
      }
      .flatMap { numbers =>
        sum.set(Some(numbers.sum))
      }

    div(
      md"`ZIO.foreach`",
      div(
        display.flex,
        alignItems.center,
        marginBottom := "1em",
        numbers.map(_.render),
        div(
          "→",
          opacity := "0.4",
          marginRight := "12px"
        ),
        sum.render
      ),
      md"""
```scala
def addFive(number: ZVar[Int]) = number.update(_ + 5)

val addFiveToAll = ZIO.foreach(numbers)(addFive).map(_.sum)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = addFiveToAll
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (addFiveToAll *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

  lazy val foreachPar = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val n4      = ZVar(25)
    val n5      = ZVar(30)
    val sum     = ZVar.result(Option.empty[Int])
    val numbers = List(n1, n2, n3, n4, n5)
    val running = Var(false)

    def addFive(number: ZVar[Int]) = number.update(_ + 5)

    val addFiveToAllPar: URIO[Clock, Unit] = ZIO
      .foreachPar(numbers) { n =>
        addFive(n) *> UIO(n.now)
      }
      .flatMap { numbers =>
        sum.set(Some(numbers.sum))
      }

    div(
      md"""
`ZIO.foreachPar`

Like `foreach`, only it executes in parallel.
        """,
      div(
        display.flex,
        alignItems.center,
        marginBottom := "1em",
        numbers.map(_.render),
        div(
          "→",
          opacity := "0.4",
          marginRight := "12px"
        ),
        sum.render
      ),
      md"""
```scala
def addFive(number: ZVar[Int]) = number.update(_ + 5)

val addFiveToAll = ZIO.foreachPar(numbers)(addFive).map(_.sum)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = addFiveToAll
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (addFiveToAllPar *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

  lazy val foreach_ = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val n4      = ZVar(25)
    val n5      = ZVar(30)
    val numbers = List(n1, n2, n3, n4, n5)
    val running = Var(false)

    def addFive(number: ZVar[Int]): URIO[Clock, Unit] = number.update(_ + 5)

    val addFiveToAll: URIO[Clock, Unit] = ZIO
      .foreach_(numbers) { n =>
        addFive(n)
      }

    div(
      md"`ZIO.foreach_`",
      div(
        display.flex,
        marginBottom := "1em",
        numbers.map(_.render)
      ),
      md"""
```scala
def addFive(number: ZVar[Int]) = number.update(_ + 5)

val addFiveToAll_ = ZIO.foreach_(numbers)(addFive)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = addFiveToAll_
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (addFiveToAll *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

  lazy val foreachPar_ = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val n4      = ZVar(25)
    val n5      = ZVar(30)
    val numbers = List(n1, n2, n3, n4, n5)
    val running = Var(false)

    def addFive(number: ZVar[Int]) = number.update(_ + 5)

    val addFiveToAllPar: URIO[Clock, Unit] = ZIO
      .foreachPar_(numbers) { n =>
        addFive(n)
      }

    div(
      md"`ZIO.foreachPar_`",
      div(
        display.flex,
        marginBottom := "1em",
        numbers.map(_.render)
      ),
      md"""
```scala
def addFive(number: ZVar[Int]) = number.update(_ + 5)

val addFiveToAllPar_ = ZIO.foreachPar_(numbers)(addFive)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = addFiveToAllPar_
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (addFiveToAllPar *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

  lazy val forkTwice = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val numbers = List(n1, n2, n3)
    val running = Var(false)

    def addFive(number: ZVar[Int]) = number.update(_ + 5) *> UIO(number.now)
    def addOne(number: ZVar[Int])  = number.update(_ + 1) *> UIO(number.now)

    val basicForking: ZIO[Clock with Random, Nothing, Unit] =
      for {
        _       <- addFive(n1).forever.fork
        divisor <- random.nextIntBetween(3, 8)
        _       <- n3.set(divisor)
        _       <- addOne(n2).delay(300.millis).doUntil(_ % divisor == 0)
      } yield ()

    div(
      md"""
`.fork`
        """,
      div(
        display.flex,
        marginBottom := "1em",
        numbers.map(_.render),
      ),
      md"""
```scala
val addWhileForked =
  for {
    _       <- addFive(firstNumber).forever.fork
    divisor <- random.nextIntBetween(3, 8)
    _       <- thirdNumber.set(divisor)
    _       <- addOne(secondNumber).doUntil(_ % divisor == 0)
  } yield ()
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = addWhileForked
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (basicForking *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

  lazy val race = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val answer  = ZVar.result(Option.empty[Int])
    val numbers = List(n1, n2, n3)
    val running = Var(false)

    def addOne(number: ZVar[Int]) = number.update(_ + 1) *> UIO(number.now)

    def addUntilDivisibleBy(number: ZVar[Int], divisor: Int) =
      addOne(number).delay(300.millis).doUntil(_ % divisor == 0)

    val raceExample: ZIO[Clock with Random, Nothing, Unit] =
      for {
        divisor <- random.nextIntBetween(5, 15)
        _       <- n3.set(divisor)
        result  <- addUntilDivisibleBy(n1, divisor) race addUntilDivisibleBy(n2, divisor)
        _       <- answer.set(Some(result))
      } yield ()

    div(
      md"""
`.race`
        """,
      div(
        display.flex,
        marginBottom := "1em",
        numbers.map(_.render),
        alignItems.center,
        div(
          "→",
          opacity := "0.4",
          marginRight := "12px"
        ),
        answer.render
      ),
      md"""
```scala
def addOne(number: ZVar[Int]) : UIO[Int] = number.updateAndGet(_ + 1)

def addUntilDivisibleBy(number: ZVar[Int], divisor: Int) : UIO[Int] =
  addOne(number).doUntil(_ % divisor == 0)

val raceExample =
  for {
    divisor <- random.nextIntBetween(5, 15)
    _       <- thirdNumber.set(divisor)
    result  <- addUntilDivisibleBy(firstNumber, divisor) race addUntilDivisibleBy(secondNumber, divisor)
  } yield result
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = raceExample
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (raceExample *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

}

case class ZVar[A] private (variable: Var[A], isResult: Boolean = false) {
  private val isUpdating  = Var(false)
  private val $isUpdating = isUpdating.signal

  def withUpdate(f: => Unit): URIO[Clock, Unit] =
    (UIO(isUpdating.set(true)) *>
      UIO(f).delay(50.millis) *>
      UIO(isUpdating.set(false)).delay(300.millis))
      .onInterrupt(UIO(isUpdating.set(false)))

  def set(a: A): URIO[Clock, Unit]         = withUpdate { variable.set(a) }
  def update(f: A => A): URIO[Clock, Unit] = withUpdate { variable.update(f) }
  def now: A                               = variable.now()
  def signal: StrictSignal[A]              = variable.signal

  def $opacity: Signal[Double] = {
    variable match {
      case _: Var[Option[_]] =>
        spring(signal.map {
          case a: Option[_] if a.isEmpty => 0.5
          case _                         => 1.0
        })
      case _ =>
        Val(1.0)
    }
  }

  def render: ReactiveHtmlElement[html.Div] =
    div(
      padding := "8px",
      opacity <-- $opacity,
      Option.when(!isResult)(marginRight := "12px"),
      borderRadius := "4px",
      background <-- spring($isUpdating.map(b => if (b) 1.0 else 0.2)).map(v =>
        s"rgba(80,${if (isResult) 160 else 80},${if (!isResult) 160 else 80},$v)"),
      child.text <-- variable.signal.map {
        case Some(a)      => a.toString
        case _: None.type => "_"
        case a            => a.toString
      }
    )
}

object ZVar {
  def apply[A](value: A): ZVar[A]  = ZVar[A](variable = Var(value))
  def result[A](value: A): ZVar[A] = ZVar[A](variable = Var(value), isResult = true)
}
