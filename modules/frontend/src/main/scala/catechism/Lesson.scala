package catechism

import blogus.components
import blogus.components.codeBlock
import catechism.ZioCatechism.track
import catechism.ZioSyntax.ZioOps
import com.raquo.laminar.api.L.{track => _, _}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import zio._
import zio.duration.durationInt

case class Lesson[A](
    name: String,
    runName: String,
    code: String,
    effect: ZIO[ZEnv, Nothing, Unit],
    arguments: Seq[Renderable],
    result: Option[ZVar[Option[A]]] = None,
    lesson: Option[ReactiveHtmlElement.Base] = None
) {
  val running = Var(false)

  def runEffect: URIO[zio.ZEnv, Unit] =
    for {
      _ <- UIO(running.set(true))
      _ <- ZIO.foreachPar_(arguments)(_.reset)
      _ <- result.map(_.reset).getOrElse(UIO.unit)
      _ <- effect
      _ <- UIO(running.set(false)).delay(300.millis)
    } yield ()

  def render: ReactiveHtmlElement.Base =
    div(
      track(name),
      div(
        components.inlineCode(name),
        marginBottom := "1em"
      ),
      lesson,
      div(
        display.flex,
        alignItems.center,
        marginBottom := "1em",
        arguments.map(_.render),
        result.map { r =>
          Seq(
            div(
              "→",
              opacity := "0.4",
              marginRight := "12px"
            ),
            r.render
          )
        }
      ),
      codeBlock(code.trim),
      div(
        cls <-- running.signal.map(b => if (b) "main running" else "main"),
        codeBlock(s"def run = ${runName}"),
        cursor.pointer,
        onClick.filter(_ => !running.now()) --> { _ =>
          runEffect.runAsync
        }
      ),
    )
}
