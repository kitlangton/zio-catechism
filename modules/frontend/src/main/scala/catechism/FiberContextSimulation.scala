package catechism

import java.util.UUID

import Transitions.splitTransition
import animator.Animator.spring
import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import com.raquo.laminar.ext.CSS._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import catechism.ZioSyntax.ZioOps
import zio._
import zio.clock.Clock
import zio.duration._
import ObservableSyntax._
import TransitionStatus._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Promise}
import scala.util.Try

object Render {
  val effect: XIO[Int] =
    for {
      x <- XIO.succeed(5).map(Add(10))
      y <- XIO.succeed(10)
      z <- XIO.succeed(10).map(Add(30))
    } yield x + y + z

  lazy val context = new FiberContext[Int]
  lazy val $stack  = context.stack.$signal

  lazy val stackView =
    div(
      div(
        div(
          opacity := "0.6",
          padding := "4px 8px",
          "EMPTY",
          fontStyle.italic
        ),
        fontSize := "0.8rem",
        opacity := "0.6",
        overflowY.hidden,
        onMountBind { el =>
          maxHeight <-- spring($stack.map(_.isEmpty).map {
            case true => el.thisNode.ref.scrollHeight.toDouble
            case _    => 0.0
          }).px
        },
      ),
      div(
        display.flex,
        flexDirection.columnReverse,
        borderRadius := "4px",
        background <-- spring(context.$highlightStack.map(b => if (b) 100.0 else 70.0)).map(d => s"rgb($d,$d,$d)"),
        children <-- splitTransition($stack.map(_.map(_.toString()).zipWithIndex))((a: (String, Int)) => a._2) {
          (idx, init, $signal, $status) =>
            val brightness = idx * 15
            div(
              overflowY.hidden,
              opacity <-- spring($status.map {
                case Active => 1.0
                case _      => 0.0
              }).string,
              inContext { el =>
                maxHeight <-- spring($status.map {
                  case Active => el.ref.scrollHeight.toDouble
                  case _      => 0.0
                }).px
              },
              div(
                background := s"rgba($brightness,$brightness,$brightness,0.5)",
                padding := "8px",
                init._1
              )
            )
        }
      )
    )

  lazy val main = {
    div(
      minHeight := "80vh",
      div(
        top := "0",
        left := "-30px",
        "EFFECT",
        fontSize := "1rem",
        opacity := "0.7",
        fontVariant.smallCaps,
      ),
      md"""
```scala
for {
  x <- ZIO.succeed(5).map(_ + 10)
  y <- ZIO.succeed(10)
  z <- ZIO.succeed(10).map(_ + 30)
} yield x + y + z
```
""",
      div(
        top := "0",
        left := "-30px",
        "STACK",
        fontSize := "1rem",
        opacity := "0.7",
        fontVariant.smallCaps,
      ),
      stackView,
      div(
        top := "0",
        left := "-30px",
        "CURRENT ZIO",
        fontSize := "1rem",
        opacity := "0.7",
        fontVariant.smallCaps,
        marginTop := "12px",
      ),
      div(
        div(
          div(
            opacity := "0.6",
            padding := "4px 8px",
            "NONE",
            fontStyle.italic
          ),
          fontSize := "0.8rem",
          opacity := "0.6",
          overflowY.hidden,
          onMountBind { el =>
            maxHeight <-- spring(context.$curXio.map(_.isEmpty).map {
              case true => el.thisNode.ref.scrollHeight.toDouble
              case _    => 0.0
            }).px
          },
        ),
        children <-- splitTransition(context.$curXio.map(_.toSeq))((a: XIO[_]) => a.uuid) {
          (key, init, $value, $status) =>
            div(
              overflowY.hidden,
              opacity <-- spring($status.map {
                case Active => 1.0
                case _      => 0.0
              }).string,
              inContext { el =>
                maxHeight <-- spring($status.map {
                  case Active => el.ref.scrollHeight.toDouble
                  case _      => 0.0
                }).px
              },
              background := "#222",
              borderRadius := "4px",
              div(
                padding := "8px",
                child <-- $value.map(_.render)
              )
            )

        }
      ),
      div(
        top := "0",
        left := "-30px",
        "ALGORITHM",
        fontSize := "1rem",
        opacity := "0.7",
        fontVariant.smallCaps,
        marginTop := "12px",
      ),
      children <-- splitTransition(context.$currentAction.map(_.toSeq))(a => a.uuid) { (key, init, $value, $status) =>
        div(
          overflowY.hidden,
          opacity <-- spring($status.map {
            case Active => 1.0
            case _      => 0.0
          }).string,
          inContext { el =>
            maxHeight <-- spring($status.map {
              case Active => el.ref.scrollHeight.toDouble
              case _      => 0.0
            }).px
          },
          background := "#222",
          borderRadius := "4px",
          div(
            padding := "8px",
            child <-- $value.map(_.render)
          )
        )
      },
      div(
        opacity <-- spring(context.isExecuting.signal.map(b => if (b) 0.0 else 1.0)),
        button(
          "BEGIN",
          disabled <-- context.isExecuting.signal,
          onClick --> { _ =>
            context.evaluateNow(effect).runAsync
          }
        )
      )
    )
  }
}

sealed trait XIO[+A] { self =>
  val uuid: String = UUID.randomUUID().toString

  def render: ReactiveHtmlElement.Base
  def flatMap[B](k: A => XIO[B]): XIO[B] = XIO.FlatMap(self, k)
  def map[B](f: A => B): XIO[B]          = XIO.FlatMap(self, MapFn(f))
}

case class Add(x: Int) extends ((Int) => Int) {
  override def toString(): String = sig("_")
  def sig(arg: String): String    = s"$x + $arg"
  override def apply(y: Int): Int = x + y
}

case class MapFn[A, B](f: A => B) extends (A => XIO[B]) {
  override def toString(): String  = s"(a) => ZIO.Succeed(($f)(a)"
  override def apply(a: A): XIO[B] = XIO.Succeed(f(a))
}

object XIO {
  def apply[A](a: => A): XIO[A]   = XIO.EffectTotal(() => a)
  def succeed[A](a: => A): XIO[A] = XIO.Succeed(a)

  case class FlatMap[A0, A](xio: XIO[A0], k: A0 => XIO[A]) extends XIO[A] {
    val highlightNested  = Var(false)
    val $highlightNested = highlightNested.signal

    val highlightContinuation  = Var(false)
    val $highlightContinuation = highlightContinuation.signal

    def render: ReactiveHtmlElement[html.Div] = div(
      display.flex,
      alignContent.center,
      alignItems.center,
      div(fontWeight := "600", "FlatMap"),
      div(width := "12px"),
      div(
        "nested",
        padding := "2px 8px",
        borderRadius := "4px",
        background <-- spring($highlightNested.map(b => if (b) 1.0 else 0.3)).map(v => s"rgba(80,160,80,$v)")
      ),
      div(width := "12px"),
      div(
        "continuation",
        padding := "2px 8px",
        borderRadius := "4px",
        background <-- spring($highlightContinuation.map(b => if (b) 1.0 else 0.3)).map(v => s"rgba(80,80,160,$v)")
      )
    )
  }
  case class Succeed[A](value: A) extends XIO[A] {
    val highlightValue  = Var(false)
    val $highlightValue = highlightValue.signal

    def render: ReactiveHtmlElement[html.Div] = div(
      display.flex,
      alignContent.center,
      alignItems.center,
      div(fontWeight := "600", "Succeed"),
      div(width := "12px"),
      div(
        padding := "2px 8px",
        borderRadius := "4px",
        background := "#224",
        value.toString,
        background <-- spring($highlightValue.map(b => if (b) 1.0 else 0.3)).map(v => s"rgba(80,80,160,$v)")
      ),
    )
  }
  case class EffectTotal[A](effect: () => A) extends XIO[A] {
    def render: ReactiveHtmlElement[html.Div] = div(
      display.flex,
      alignContent.center,
      alignItems.center,
      div(fontWeight := "600", "EffectTotal"),
      div(width := "12px"),
      div(
        "effect",
        padding := "2px 8px",
        borderRadius := "4px",
        background := "#224"
      ),
    )
  }
}

final class Stack[A] {
  private val array = ArrayBuffer.empty[A]

  private val bus                  = new EventBus[Seq[A]]
  lazy val $signal: Signal[Seq[A]] = bus.events.startWith(Seq.empty)

  private def withUpdate[B](f: () => B): B = {
    val result = f()
    bus.writer.onNext(array.toSeq)
    result
  }

  def isEmpty: Boolean = array.isEmpty
  def peek(): A        = array.last
  def push(a: A): Unit = withUpdate { () =>
    array.append(a)
  }
  def pop(): A = withUpdate { () =>
    array.remove(array.length - 1)
  }
}

object Stack {
  def apply[A]() = new Stack[A]
}
final class FiberContext[A] {
  val stack                          = Stack[Any => XIO[Any]]()
  val isExecuting                    = Var(false)
  val curXio0: Var[Option[XIO[Any]]] = Var(None)

  val highlightStack  = Var(false)
  val $highlightStack = highlightStack.signal

  lazy val $curXio        = curXio0.signal
  lazy val currentAction  = Var(Option.empty[ZioAction[A]])
  lazy val $currentAction = currentAction.signal

  private val callbacks = ArrayBuffer.empty[A => Unit]

  def register(callback: A => Unit): Unit = callbacks.append(callback)

  def evaluateNow(xio0: XIO[Any]): URIO[Clock, Unit] = {
    callbacks.clear()
    isExecuting.set(true)
    curXio0.set(Some(xio0))
    eval()
  }

  def pushToStack(k: Any => XIO[Any]): URIO[Clock, Unit] =
    UIO(highlightStack.set(true)) *> ZIO.sleep(100.millis) *> UIO(stack.push(k)) *> ZIO.sleep(100.millis) *>
      UIO(highlightStack.set(false))

  def popFromStack(): URIO[Clock, Any => XIO[Any]] =
    for {
      _      <- UIO(highlightStack.set(true))
      _      <- ZIO.sleep(100.millis)
      result <- UIO(stack.pop())
      _      <- ZIO.sleep(100.millis)
      _      <- UIO(highlightStack.set(false))
    } yield result

  def eval(): URIO[Clock, Unit] = {
    def go(): ZIO[Clock, Nothing, Unit] = {
      curXio0.now() match {
        case Some(curXio) =>
          val action = ZioAction.fromZio[A](curXio, stack)
          currentAction.set(Some(action))
          action.execute(this)
        case None =>
          ZIO.unit
      }
    }

    val continue = for {
      next <- UIO(curXio0.now())
      _ <- ZIO.whenCase(next) {
        case Some(_) => eval()
        case None    => ZIO.unit
      }
    } yield ()

    go() *> continue.delay(100.millis)
  }

  def done(value: A): Option[XIO[Any]] = {
    callbacks.foreach(cb => cb(value))
    isExecuting.set(false)
    None
  }

}

object FiberContext {
  def run[A](xio: XIO[A]): A = {
    val context = new FiberContext[A]
    val promise = Promise[A]()
    context.register { value =>
      promise.complete(Try(value))
    }
    context.evaluateNow(xio)
    Await.result(promise.future, 30.seconds.asScala)
  }
}

case class Step[A](description: String, action: FiberContext[A] => URIO[Clock, Unit]) {
  def render = div(
    description
  )
}

trait ZioAction[A] {
  val uuid: String   = UUID.randomUUID().toString
  var currentStepIdx = Var(0)
  def steps: Seq[Step[A]]

  def render: ReactiveHtmlElement[html.Div] = div(
    steps.zipWithIndex.map {
      case (step, index) =>
        val isActive = currentStepIdx.signal.map(_ == index)
        div(
          opacity <-- spring(isActive.map(b => if (b) 1.0 else 0.6)),
          display.flex,
          div(
            opacity := "0.7",
            fontVariant.smallCaps,
            s"${index + 1}."
          ),
          div(width := "8px"),
          step.render,
        )
    }
  )

  def execute(fiberContext: FiberContext[A]): ZIO[Clock, Nothing, Unit] =
    ZIO
      .collect(steps) { step =>
        for {
          _ <- ZIO.sleep(500.millis)
          _ <- step.action(fiberContext)
          _ <- ZIO.sleep(1.second)
          _ <- UIO(currentStepIdx.update(_ + 1))
        } yield ()
      }
      .unit
}

object ZioAction {
  def fromZio[A](xio: XIO[_], stack: Stack[_]): ZioAction[A] = xio match {
    case map: XIO.FlatMap[_, _] =>
      FlatMapAction(map)
    case succeed: XIO.Succeed[A] =>
      if (stack.isEmpty) SucceedActionEmpty(succeed)
      else SucceedActionNonEmpty(succeed)
    case total: XIO.EffectTotal[A] =>
      if (stack.isEmpty) EffectTotalActionEmpty(total)
      else EffectTotalActionNonEmpty(total)
  }
}

case class FlatMapAction[A](xio: XIO.FlatMap[_, _]) extends ZioAction[A] {
  override def steps: Seq[Step[A]] = Seq(
    Step(
      "Push the continuation to the stack", { context: FiberContext[A] =>
        for {
          _ <- UIO(xio.highlightContinuation.set(true))
          _ <- context.pushToStack(xio.k.asInstanceOf[Any => XIO[Any]]).delay(300.millis)
          _ <- UIO(xio.highlightContinuation.set(false)).delay(300.millis)
        } yield ()
      }
    ),
    Step(
      "Set currZio to the nested ZIO", { context: FiberContext[A] =>
        for {
          _ <- UIO(xio.highlightNested.set(true))
          _ <- UIO(context.curXio0.set(Some(xio.xio))).delay(500.millis)
          _ <- UIO(xio.highlightNested.set(false))
        } yield ()
      }
    ),
  )
}

case class SucceedActionEmpty[A](xio: XIO.Succeed[A]) extends ZioAction[A] {
  override def steps: Seq[Step[A]] = Seq(
    Step("Call the callbacks with the result", { context: FiberContext[A] =>
      UIO(context.done(xio.value))
    })
  )
}

case class SucceedActionNonEmpty[A](xio: XIO.Succeed[_]) extends ZioAction[A] {
  var k: Any => XIO[Any] = _

  override def steps: Seq[Step[A]] = Seq(
    Step("Pop the next continuation off the stack", { context: FiberContext[A] =>
      context.popFromStack().flatMap { result =>
        UIO { k = result }
      }
    }),
    Step(
      "Set current to the result of calling the continuation with the value", { context: FiberContext[A] =>
        UIO(xio.highlightValue.set(true)) *>
          UIO(context.curXio0.set(Some(k(xio.value)))).delay(500.millis)
      }
    ),
  )
}

case class EffectTotalActionEmpty[A](xio: XIO.EffectTotal[A]) extends ZioAction[A] {
  override def steps: Seq[Step[A]] = Seq(
    Step("Call the callbacks with the result of calling the effect", { context: FiberContext[A] =>
      UIO(context.done(xio.effect()))
    })
  )
}

case class EffectTotalActionNonEmpty[A](xio: XIO.EffectTotal[_]) extends ZioAction[A] {
  var k: Any => XIO[Any] = _

  override def steps: Seq[Step[A]] = Seq(
    Step("Pop the next continuation off the stack", { context: FiberContext[A] =>
      context.popFromStack().flatMap { result =>
        UIO { k = result }
      }
    }),
    Step(
      "Set current to the result of calling the continuation with the effect's result", { context: FiberContext[A] =>
        UIO(context.curXio0.set(Some(k(xio.effect()))))
      }
    ),
  )
}
