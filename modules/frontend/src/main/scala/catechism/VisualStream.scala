package catechism

import animator.Color
import catechism.ObservableSyntax.{NumericSignalOps, SignalOps}
import com.raquo.laminar.api.L._
import zio._
import zio.duration._
import zio.stream.{UStream, ZStream}

case class VisualStream[A](ustream: UStream[A]) {
  lazy val valuesVar: Var[List[(A, Long)]] = Var(List.empty)

  def runDrain: UIO[Unit] =
    stream.runDrain

  def map[B](f: A => B): VisualStream[B] =
    VisualStream[B](stream.map(f))

  def filter(f: A => Boolean): VisualStream[A] =
    VisualStream[A](stream.filter(f))

  def stream: ZStream[Any, Nothing, A] =
    ustream.zipWithIndex
      .tap {
        case (a, idx) =>
          UIO {
            VisualStream.count.update(_ max idx)
            valuesVar.update { list =>
              ((a, VisualStream.count.now()) :: list).take(10)
            }
          }
      }
      .map(_._1)

  def view: Div = div(
    overflow.hidden,
    display.flex,
    position.relative,
    alignItems.center,
    background("rgba(10,10,10,0.12)"),
    border("1px solid #222"),
    margin("12px 0px"),
    borderRadius("8px"),
    width("100%"),
    padding("12px"),
    height("60px"),
    justifyContent.flexEnd,
    children <-- valuesVar.signal.split(_._2) {
      case (_, (value, idx), _) =>
        val clock: Signal[Double] = VisualStream.count.signal.map(_.toDouble).map(_ - idx - 0.8)

        val $backgroundColor =
          clock
            .map { i =>
              if (i > 1) Color(40, 40, 60)
              else if (i > 0) Color(70, 70, 130)
              else Color(120, 120, 200)
            }
            .composeChanges(_.delay(300))
            .spring

        val $scale = clock
          .map { i =>
            if (i < 0) 1.3 else 1.0
          }
          .spring
          .map(s => s"scale($s)")

        val $opacity = clock.map { i =>
          if (i < 0) 0.0 else 1.0
        }.spring

        div(
          fontSize("22px"),
          display.flex,
          alignItems.center,
          justifyContent.center,
          opacity <-- $opacity,
          transform <-- $scale,
          height("40px"),
          width("80px"),
          background <-- $backgroundColor.map(_.css),
          borderRadius("4px"),
          position.absolute,
          right <-- clock.map(_ * 92.0 - 1 - 7).spring.px,
          value.toString
        )
    },
    div(
      position.absolute,
      zIndex(10000),
      left("0"),
      top("0"),
      right("0"),
      bottom("0"),
      background("linear-gradient(to right, rgba(10,10,10,0.9), rgb(0, 0, 0, 0) 52%)")
    ),
  )
}

object VisualStream {
  lazy val count: Var[Long] = Var(0L)

  lazy val numbers: VisualStream[Int] = {
    val numberStream = for {
      ref <- ZStream.fromEffect(zio.Ref.make(1))
      stream <- ZStream.succeed(0) ++ ZStream
        .repeatEffect(ref.getAndUpdate(r => (r + 1) % 100).delay(1.second))
        .provideLayer(ZEnv.live)
    } yield stream

    VisualStream(numberStream)
  }

  lazy val randomInt: VisualStream[Int] = {
    VisualStream(
      (ZStream.fromEffect(random.nextIntBetween(1, 100)) ++
        ZStream
          .repeatEffect(random.nextIntBetween(1, 100).delay(1.second)))
        .provideLayer(ZEnv.live)
    )
  }

  lazy val letters: VisualStream[String] = {
    val letters = ('A' to 'Z').toVector
    VisualStream(for {
      ref <- ZStream.fromEffect(zio.Ref.make(1))
      stream <- (ZStream.succeed("A") ++ ZStream
        .repeatEffect(ref.getAndUpdate(r => (r + 1) % 26).map(i => letters(i).toString).delay(1.second)))
        .provideLayer(ZEnv.live)
    } yield stream)
  }
}
