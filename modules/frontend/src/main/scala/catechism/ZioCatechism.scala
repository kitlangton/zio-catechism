package catechism

import animator.Animator.spring
import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import com.raquo.laminar.api.L.{Ref => _, _}
import com.raquo.laminar.ext.CSS.fontVariant
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import catechism.ZioSyntax.ZioOps
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
    md"""### Effectful "Iteration"""",
    foreach,
    hr(opacity := "0.2"),
    foreachPar,
    hr(opacity := "0.2"),
    md"""
#### Underscore Variants
""",
    foreach_,
    hr(opacity := "0.2"),
    foreachPar_,
    hr(opacity := "0.2"),
    md"### Forking",
    forkTwice,
    hr(opacity := "0.2"),
    simpleFork,
    md"### Racing",
    simpleRace,
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

Just like `foreach`, only executed in parallel. 

***Convention** — The `Par` suffix indicates parallel execution.*
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
      md"""
`ZIO.foreach_`
      
***Convention** — Methods endings with an underscore (i.e., `foreach_`) return `Unit`.
These will also be more performant, as they do not build up a list of results.*
      """,
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
        alignItems.center,
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

  lazy val simpleFork = {
    val randomNumber = random.nextIntBetween(10, 99)
    val n1           = VisualTask(randomNumber)
    val n2           = VisualTask(randomNumber)

    val answer  = ZVar.result(Option.empty[Int])
    val numbers = List(n1, n2)
    val running = Var(false)

    val raceExample: ZIO[Clock with Random, Nothing, Unit] =
      for {
        _     <- n1.reset <&> n2.reset
        fiber <- n1.runRandom(2.seconds, 4.seconds).fork
        x     <- n2.run(800.millis).delay(300.millis)
        y     <- fiber.join
        _     <- answer.set(Some(x + y)).delay(300.millis)
      } yield ()

    div(
      md"`.fork` and `.join`",
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
val forkExample: ZIO[Clock with Random, Nothing, Int] =
  for {
    fiber <- MechanicalTurkRandomNumberGenerator.get.fork
    x     <- FastRandomNumberGenerator.get
    y     <- fiber.join
  } yield x + y
```
""",
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        md"""
```scala
def run = forkExample
```
      """,
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          running.set(true)
          answer.set(None).runAsync
          (raceExample *>
            UIO(running.set(false)).delay(300.millis)).runAsync
        }
      ),
    )
  }

  lazy val simpleRace = {
    val randomNumber = random.nextIntBetween(10, 99)
    val n1           = VisualTask(randomNumber)
    val n2           = VisualTask(randomNumber)

    val answer  = ZVar.result(Option.empty[Int])
    val numbers = List(n1, n2)
    val running = Var(false)

    val raceExample: ZIO[Clock with Random, Nothing, Unit] =
      for {
        result <- n1.runRandom() race n2.runRandom()
        _      <- answer.set(Some(result)).delay(300.millis)
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
val randomNumber : UIO[Int] = FlakyRandomNumberService.get

val raceExample: UIO[Int] = randomNumber race randomNumber
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
          answer.set(None).runAsync
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
