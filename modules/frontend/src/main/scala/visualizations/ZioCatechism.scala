package visualizations

import animator.Animator.spring
import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import com.raquo.laminar.api.L.{Ref => _, _}
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
      "catechism",
      opacity := "0.7",
      fontSize := "1.4rem",
    ),
    textAlign.center,
    fontVariant.smallCaps,
    marginBottom := "30px"
  )

  lazy val main = div(
    maxWidth := "650px",
    margin := "20px auto",
    padding := "0px 12px",
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
    br(),
    br(),
    footer
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

    def add5(number: ZVar[Int]) = number.update(_ + 5)

    val add5ToAll: URIO[Clock, Unit] = ZIO
      .foreach(numbers) { n =>
        add5(n) *> n.get
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
def add5(number: Ref[Int]) : UIO[Int] = number.updateAndGet(_ + 5)

val add5ToAll : UIO[Int] = ZIO.foreach(numbers)(add5).map(_.sum)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = add5ToAll
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (add5ToAll *>
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

    def add5(number: ZVar[Int]) = number.update(_ + 5)

    val add5ToAllPar: URIO[Clock, Unit] = ZIO
      .foreachPar(numbers) { n =>
        add5(n) *> n.get
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
def add5(number: Ref[Int]) : UIO[Int] = number.updateAndGet(_ + 5)

val add5ToAllPar : UIO[Int] = ZIO.foreachPar(numbers)(add5).map(_.sum)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = add5ToAllPar
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (add5ToAllPar *>
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

    def add5(number: ZVar[Int]): URIO[Clock, Unit] = number.update(_ + 5)

    val add5ToAll: URIO[Clock, Unit] = ZIO
      .foreach_(numbers) { n =>
        add5(n)
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
def add5(number: ZVar[Int]): UIO[Int] = number.updateAndGet(_ + 5)

val add5ToAll_: UIO[Unit] = ZIO.foreach_(numbers)(add5)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = add5ToAll_
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (add5ToAll *>
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

    def add5(number: ZVar[Int]) = number.update(_ + 5)

    val add5ToAllPar: URIO[Clock, Unit] = ZIO
      .foreachPar_(numbers) { n =>
        add5(n)
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
def add5(number: Ref[Int]): UIO[Int] = number.updateAndGet(_ + 5)

val add5ToAllPar_: UIO[Unit] = ZIO.foreachPar_(numbers)(add5)
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = add5ToAllPar_
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          (add5ToAllPar *>
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

    def add5(number: ZVar[Int])   = number.update(_ + 5) *> (number.get)
    def addOne(number: ZVar[Int]) = number.update(_ + 1) *> (number.get)

    val basicForking: ZIO[Clock with Random, Nothing, Unit] =
      for {
        _       <- n1.interrupt(false)
        _       <- add5(n1).forever.onInterrupt(n1.interrupt()).fork
        divisor <- random.nextIntBetween(3, 8)
        _       <- n3.set(divisor)
        _       <- addOne(n2).delay(300.millis).doUntil(_ % divisor == 0)
      } yield ()

    div(
      md"`.fork`",
      div(
        display.flex,
        marginBottom := "1em",
        numbers.map(_.render),
      ),
      md"""
```scala
val addWhileForked : UIO[Unit] =
  for {
    _       <- add5(n1).forever.fork
    x       <- random.nextIntBetween(3, 8)
    _       <- divisor.set(x)
    _       <- addOne(n2).doUntil(_ % x == 0)
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
    val n1      = ZVar(7)
    val n2      = ZVar(13)
    val n3      = ZVar(17)
    val n4      = ZVar(20)
    val answer  = ZVar.result(Option.empty[Int])
    val numbers = List(n1, n2, n3, n4)
    val running = Var(false)

    def addOne(number: ZVar[Int]) = number.update(_ + 1) *> (number.get)

    def addUntilDivisibleBy(number: ZVar[Int], divisor: Int) =
      addOne(number).delay(300.millis).doUntil(_ % divisor == 0).onInterrupt(number.interrupt())

    val raceExample: ZIO[Clock with Random, Nothing, Unit] =
      for {
        _       <- ZIO.collectAll_(numbers.map(_.interrupt(false)))
        divisor <- random.nextIntBetween(5, 15)
        _       <- n4.set(divisor)
        result <- addUntilDivisibleBy(n1, divisor) race
          addUntilDivisibleBy(n2, divisor) race addUntilDivisibleBy(n3, divisor)
        _ <- answer.set(Some(result))
      } yield ()

    div(
      md"`.race`",
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
def addOne(number: Ref[Int]): UIO[Int] = number.updateAndGet(_ + 1)

def untilDivisible(number: Ref[Int], divisor: Int): UIO[Int] =
  addOne(number).doUntil(_ % divisor == 0)

val raceExample: UIO[Int] =
  for {
    x      <- random.nextIntBetween(5, 15)
    _      <- divisor.set(x)
    result <- untilDivisible(n1, x) race untilDivisible(n2, x) race untilDivisible(n3, x)
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

  lazy val footer = div(
    md"""
🐙 Contribute on [GitHub](https://github.com/kitlangton/zio-catechism)
      """,
  )
}
