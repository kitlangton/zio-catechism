package catechism

import blogus.components
import blogus.components.codeBlock
import catechism.ZioCatechism.track
import catechism.ZioSyntax.ZioOps
import com.raquo.laminar.api.L.{track => _, _}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import zio._
import zio.duration.durationInt

case class Lesson[E, A](
    name: String,
    runName: String,
    code: String,
    effect: ZIO[ZEnv, E, Unit],
    arguments: Seq[Renderable],
    result: Option[ZVar[E, Option[A]]] = None,
    lesson: Option[ReactiveHtmlElement.Base] = None
) {
  val running = Var(false)

  def runEffect: ZIO[zio.ZEnv, E, Unit] =
    (for {
      _ <- UIO(running.set(true))
      _ <- ZIO.foreachPar_(arguments)(_.reset)
      _ <- result.map(_.reset).getOrElse(UIO.unit)
      _ <- effect
    } yield ())
      .ensuring(UIO(running.set(false)).delay(300.millis))
      .catchAll { error =>
        ZIO.fromOption(result).map(_.error.set(Some(error))).ignore
      }

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
