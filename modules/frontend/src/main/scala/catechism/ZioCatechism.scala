package catechism

import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import catechism.SignalSyntax.{BooleanSignalOps, NumericSignalOps}
import catechism.ZioSyntax.ZioOps
import com.raquo.laminar.api.L.{Ref => _, _}
import com.raquo.laminar.ext.CSS._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html
import zio._
import zio.clock.Clock
import zio.duration._
import zio.random.Random

object ZioCatechism {
  // This seems to eagerly load some lazy variables, which otherwise caused the
  // execution of an effect lag the first time user clicks a run button.
  private val _ = UIO(()).runAsync

  case class Trackable(name: String, element: dom.Element)
  private val elements = Var(Seq.empty[Trackable])

  lazy val $activeElement: Signal[Option[Trackable]] = $scrollPosition.signal.combineWith(elements.signal).map {
    case (_, elements) =>
      elements.find(_.element.getBoundingClientRect().bottom > 0)
  }

  def track(name: String): Modifier[Element] = onMountCallback { el =>
    val trackable = Trackable(name, el.thisNode.ref)
    elements.update(_.appended(trackable))
  }

  private lazy val header = div(
//    position := "sticky",
//    top := "10px",
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

  val $scrollPosition = Var(0.0)

  private val searchInput: ReactiveHtmlElement[html.Div] = div(
    margin := "8px 0",
    cursor.pointer,
    div(
      opacity := "0.6",
      "SEARCH",
    ),
    background := "#333",
    padding := "4px 12px",
    borderRadius := "4px",
  )

  private lazy val sidebar = div(
    position := "fixed",
    top := "0px",
    left := "0px",
    display.flex,
    flexDirection.column,
    padding := "2px 12px",
    div(
      fontVariant.smallCaps,
      "index",
    ),
    div(
      fontSize := "0.8em",
//      opacity := "0.7",
      children <-- elements.signal.split(_.name) { (key, init, $value) =>
        val top     = () => init.element.getBoundingClientRect().top
        val $active = $activeElement.map(_.exists(_.name == init.name)).percent
        div(
          display.flex,
          position.relative,
          left <-- $active.map { _ * 10 }.px,
          opacity <-- $active.map { p =>
            0.6 + (p * 0.4)
          },
//          div(
//            opacity <-- $active,
//            width <-- $active.map { _ * 2 }.px,
//            marginRight <-- $active.map { _ * 4 }.px,
//            background := "#555"
//          ),
          cls := "nav-item",
          init.name,
          onClick --> { _ =>
            dom.window.scroll(0,
                              dom.window.pageYOffset.toInt +
                                top().toInt)
          }
        )
      }
    )
  )

  lazy val main = div(
    maxWidth := "650px",
    position.relative,
    margin := "20px auto",
    padding := "0px 12px",
    header,
    windowEvents.onScroll.mapTo(dom.window.pageYOffset) --> $scrollPosition.writer,
//    div(
//      margin := "0 auto",
//      justifyContent.center,
//      display := "grid",
//      gridGap := "18px",
//      gridTemplateColumns := "250px 1fr",
//    sidebar,
    content,
//    ),
    br(),
    br(),
    footer
  )

  lazy val content = div(
    md"""## Effects on Collections""",
    collectAllLesson.render,
    hr(opacity := "0.2"),
    collectAllParLesson.render,
    hr(opacity := "0.2"),
    collectAllParNLesson.render,
    hr(opacity := "0.2"),
    foreachLesson.render,
    hr(opacity := "0.2"),
    foreachParLesson.render,
    hr(opacity := "0.2"),
    md"""
#### Underscore Variants
""",
    foreach_Lesson.render,
    hr(opacity := "0.2"),
    foreachPar_Lesson.render,
    hr(opacity := "0.2"),
    md"## Forking",
    forkLesson.render,
    hr(opacity := "0.2"),
    forkDaemonLesson.render,
    hr(opacity := "0.2"),
    joinLesson.render,
    hr(opacity := "0.2"),
    md"## Racing",
    raceLesson.render,
  )

  def section(element: ReactiveHtmlElement.Base): ReactiveHtmlElement[html.Div] = div(
    borderLeft := "4px solid #333",
    paddingLeft := "18px",
    marginBottom := "1.5em",
    element
  )

  lazy val collectAllLesson = {
    val randomPrice = random.nextIntBetween(10, 99)
    val n1          = VisualTask(randomPrice)
    val n2          = VisualTask(randomPrice)
    val n3          = VisualTask(randomPrice)
    val n4          = VisualTask(randomPrice)
    val n5          = VisualTask(randomPrice)
    val sum         = ZVar.result[Int]
    val numbers     = List(n1, n2, n3, n4, n5)

    val add5ToAll: URIO[Random with Clock, Unit] =
      ZIO
        .foreach(numbers)(_.runRandom(300.millis, 800.millis))
        .flatMap { numbers =>
          sum.set(Some(numbers.sum)).delay(300.millis)
        }

    Lesson(
      name = "ZIO.collectAll",
      runName = "sumRandoms",
      code = """val randomNumbers: List[UIO[Int]] = 
  List.fill(5)(FlakyRandomNumberService.get)

val sumRandoms: UIO[Int] = 
  ZIO.collectAll(randomNumbers).map(_.sum)""",
      effect = add5ToAll,
      arguments = numbers,
      result = Some(sum)
    )
  }

  lazy val collectAllParLesson = {
    val randomPrice = random.nextIntBetween(10, 99)
    val n1          = VisualTask(randomPrice)
    val n2          = VisualTask(randomPrice)
    val n3          = VisualTask(randomPrice)
    val n4          = VisualTask(randomPrice)
    val n5          = VisualTask(randomPrice)
    val sum         = ZVar.result[Int]
    val numbers     = List(n1, n2, n3, n4, n5)

    val sumRandomsPar: URIO[Random with Clock, Unit] =
      ZIO
        .foreachPar(numbers)(_.runRandom(300.millis, 800.millis))
        .flatMap { numbers =>
          sum.set(Some(numbers.sum)).delay(300.millis)
        }

    Lesson(
      name = "ZIO.collectAllPar",
      runName = "sumRandomsPar",
      code = """val randomNumbers: List[UIO[Int]] = 
  List.fill(5)(FlakyRandomNumberService.get)

val sumRandomsPar: UIO[Int] = 
  ZIO.collectAllPar(randomNumbers).map(_.sum)""",
      effect = sumRandomsPar,
      arguments = numbers,
      result = Some(sum)
    )
  }

  lazy val collectAllParNLesson = {
    val randomPrice = random.nextIntBetween(10, 99)
    val n1          = VisualTask(randomPrice)
    val n2          = VisualTask(randomPrice)
    val n3          = VisualTask(randomPrice)
    val n4          = VisualTask(randomPrice)
    val n5          = VisualTask(randomPrice)
    val sum         = ZVar.result[Int]
    val numbers     = List(n1, n2, n3, n4, n5)

    val sumRandomsPar: URIO[Random with Clock, Unit] =
      ZIO
        .foreachParN(3)(numbers)(_.runRandom(300.millis, 800.millis))
        .flatMap { numbers =>
          sum.set(Some(numbers.sum)).delay(300.millis)
        }

    Lesson(
      name = "ZIO.collectAllParN",
      runName = "sumRandomsParN",
      code = """val randomNumbers: List[UIO[Int]] = 
  List.fill(5)(FlakyRandomNumberService.get)

val sumRandomsPar: UIO[Int] = 
  ZIO.collectAllParN(3)(randomNumbers).map(_.sum)""",
      effect = sumRandomsPar,
      arguments = numbers,
      result = Some(sum)
    )
  }

  def mkForEachLesson(
      suffix: String,
      lesson: Option[ReactiveHtmlElement.Base] = None,
  ): Lesson[Int] = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val n4      = ZVar(25)
    val n5      = ZVar(30)
    val sum     = ZVar.result[Int]
    val numbers = List(n1, n2, n3, n4, n5)

    def add5(number: ZVar[Int]) = number.update(_ + 5)

    def foreach(numbers1: List[ZVar[Int]])(
        function: ZVar[Int] => ZIO[Clock, Nothing, Int]): ZIO[Clock, Nothing, List[Int]] =
      if (suffix.contains("Par"))
        ZIO.foreachPar(numbers1)(function)
      else
        ZIO.foreach(numbers1)(function)

    val add5ToAll: URIO[Clock, Unit] = foreach(numbers) { n =>
      add5(n) *> n.get
    }.flatMap { numbers =>
      sum.set(Some(numbers.sum))
    }

    val resultType = if (suffix.endsWith("_")) "Unit" else "Int"

    Lesson(
      s"ZIO.foreach${suffix}",
      s"add5ToAll${suffix}",
      s"""
        |def add5(number: Ref[Int]) : UIO[Int] = 
        |  number.updateAndGet(_ + 5)
        |
        |val add5ToAll${suffix} : UIO[$resultType] = 
        |  ZIO.foreach${suffix}(numbers)(add5).map(_.sum)
        |""".stripMargin,
      add5ToAll,
      numbers,
      Option.when(!suffix.endsWith("_"))(sum),
      lesson = lesson
    )
  }

  lazy val foreachLesson = mkForEachLesson("")
  lazy val foreachParLesson = mkForEachLesson(
    "Par",
    Some(
      md"""
Just like foreach, only executed in parallel.
 
***Convention** — The `Par` suffix indicates parallel execution.*
      """
    )
  )
  lazy val foreach_Lesson = mkForEachLesson(
    "_",
    Some(
      md"""
***Convention** — Methods endings with an underscore (i.e., `foreach_`) return `Unit`.
These will also be more performant, as they do not build up a list of results.*
"""
    )
  )
  lazy val foreachPar_Lesson = mkForEachLesson("Par_")

  lazy val forkLesson = {
    val n1      = ZVar(10)
    val n2      = ZVar(15)
    val n3      = ZVar(20)
    val numbers = List(n1, n2, n3)

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

    Lesson(
      ".fork",
      "forkExample",
      """val addWhileForked : UIO[Unit] =
        |  for {
        |    _       <- add5(n1).forever.fork
        |    x       <- random.nextIntBetween(3, 8)
        |    _       <- divisor.set(x)
        |    _       <- addOne(n2).doUntil(_ % x == 0)
        |  }
        |""".stripMargin,
      basicForking,
      numbers,
    )
  }

  lazy val forkDaemonLesson = {
    val n1       = ZVar(10)
    val n2       = ZVar(15)
    val n3       = ZVar(20)
    val numbers  = List(n1, n2, n3)
    val finished = Var(false)

    def addFive(number: ZVar[Int]) = number.update(_ + 5) *> (number.get)
    def addOne(number: ZVar[Int])  = number.update(_ + 1) *> (number.get)

    val basicForking: ZIO[Clock with Random, Nothing, Unit] =
      for {
        _       <- n1.interrupt(false)
        _       <- addFive(n1).forever.forkDaemon.uninterruptible
        divisor <- random.nextIntBetween(3, 8)
        _       <- n3.set(divisor)
        _       <- addOne(n2).delay(300.millis).doUntil(_ % divisor == 0)
        _       <- UIO(finished.set(true))
      } yield ()

    val $finished = finished.signal.percent.composeChanges(_.delay(2000))

    val explanation = div(
      overflowY.hidden,
      inContext { el =>
        Seq(
          maxHeight <-- $finished.map { _ * el.ref.scrollHeight.toDouble }.px,
          opacity <-- $finished
        )
      },
      md"😈 *Yeah, that's just gonna keep running.*"
    )

    Lesson(
      ".forkDaemon",
      "forkDaemonExample",
      """val addWhileForked : UIO[Unit] =
        |  for {
        |    _       <- add5(n1).forever.forkDaemon
        |    x       <- random.nextIntBetween(3, 8)
        |    _       <- divisor.set(x)
        |    _       <- addOne(n2).doUntil(_ % x == 0)
        |  }
        |""".stripMargin,
      basicForking,
      numbers,
      lesson = Some(explanation)
    )
  }

  def joinLesson = {
    val randomNumber = random.nextIntBetween(10, 99)
    val n1           = VisualTask(randomNumber)
    val n2           = VisualTask(randomNumber)

    val answer  = ZVar.result[Int]
    val numbers = List(n1, n2)

    val joinExample: ZIO[Clock with Random, Nothing, Unit] =
      for {
        fiber <- n1.runRandom(2.seconds, 4.seconds).fork
        x     <- n2.run(800.millis).delay(300.millis)
        y     <- fiber.join
        _     <- answer.set(Some(x + y)).delay(300.millis)
      } yield ()

    Lesson(
      ".join",
      "joinExample",
      """val joinExample: ZIO[Clock with Random, Nothing, Int] =
        |  for {
        |    fiber <- MechanicalTurkRandomNumberGenerator.get.fork
        |    x     <- FastRandomNumberGenerator.get
        |    y     <- fiber.join
        |  } yield x + y
        |""".stripMargin,
      joinExample,
      numbers,
      Some(answer)
    )
  }

  def raceLesson = {
    val randomNumber = random.nextIntBetween(10, 99)
    val n1           = VisualTask(randomNumber)
    val n2           = VisualTask(randomNumber)

    val answer  = ZVar.result[Int]
    val numbers = List(n1, n2)

    val raceExample: ZIO[Clock with Random, Nothing, Unit] =
      for {
        result <- n1.runRandom() race n2.runRandom()
        _      <- answer.set(Some(result)).delay(300.millis)
      } yield ()

    Lesson(
      ".race",
      "raceExample",
      """val randomNumber : UIO[Int] = FlakyRandomNumberService.get
        |
        |val raceExample: UIO[Int] = randomNumber race randomNumber
        |""".stripMargin,
      raceExample,
      numbers,
      Some(answer)
    )
  }

  lazy val footer = div(
    md"""
🐙 Contribute on [GitHub](https://github.com/kitlangton/zio-catechism)
      """,
  )
}
