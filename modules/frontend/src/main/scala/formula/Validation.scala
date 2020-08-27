package formula

import zio.Chunk

sealed trait Validation[+E, +A] { self =>
  import Validation._

  def flatMap[E1 >: E, B](f: A => Validation[E1, B]): Validation[E1, B] =
    self match {
      case Success(value) =>
        f(value)
      case Warning(ws, a) =>
        f(a) match {
          case Success(b)              => Warning(ws, b)
          case Warning(w1s, b)         => Warning(ws ++ w1s, b)
          case failure @ Failure(_, _) => failure
        }
      case failure @ Failure(_, _) => failure
    }

  def <&>[E1 >: E, B](that: Validation[E1, B]): Validation[E1, (A, B)] = zip(that)

  final def map[B](f: A => B): Validation[E, B] =
    self match {
      case Success(a)              => Success(f(a))
      case Warning(warnings, a)    => Warning(warnings, f(a))
      case failure @ Failure(_, _) => failure
    }

  final def mapError[E1](f: E => E1): Validation[E1, A] =
    self match {
      case Failure(es, ws)      => Failure(es.map(f), ws.map(f))
      case success @ Success(_) => success
    }

  def getOrElse[A1 >: A](default: A1): A1 = self match {
    case Failure(_, _) => default
    case Success(a)    => a
    case Warning(_, a) => a
  }

  final def zipLeft[E1 >: E, B](that: Validation[E1, B]): Validation[E1, A] =
    zipWith(that)((a, _) => a)

  final def zipRight[E1 >: E, B](that: Validation[E1, B]): Validation[E1, B] =
    zipWith(that)((_, b) => b)

  def zip[E1 >: E, B](that: Validation[E1, B]): Validation[E1, (A, B)] =
    zipWith(that)((_, _))

  def zipWith[E1 >: E, B, C](that: Validation[E1, B])(f: (A, B) => C): Validation[E1, C] =
    (self, that) match {
      case (Failure(es, ws), WarningsAndErrors(e1s, w1s)) => Failure(es ++ e1s, ws ++ w1s)
      case (WarningsAndErrors(e1s, w1s), Failure(es, ws)) => Failure(es ++ e1s, ws ++ w1s)
      case (Warning(ws, a), WarningsAndValue(w1s, b))     => Warning(ws ++ w1s, f(a, b))
      case (WarningsAndValue(w1s, a), Warning(ws, b))     => Warning(ws ++ w1s, f(a, b))
      case (Success(a), Success(b))                       => Success(f(a, b))
    }
}

object Validation {
  def succeed[A](a: A): Validation[Nothing, A]       = Success(a)
  def fail[E](error: E): Validation[E, Nothing]      = Failure(Chunk(error))
  def warn[E, A](warning: E, a: A): Validation[E, A] = Warning(Chunk(warning), a)

  final case class Success[+A](value: A)                                               extends Validation[Nothing, A]
  final case class Warning[+E, +A](warnings: Chunk[E], value: A)                       extends Validation[E, A]
  final case class Failure[+E, +A](errors: Chunk[E], warnings: Chunk[E] = Chunk.empty) extends Validation[E, Nothing]

  object WarningsAndErrors {
    def unapply[E, A](validated: Validation[E, A]): Option[(Chunk[E], Chunk[E])] =
      validated match {
        case Failure(errors, warnings) => Some(errors, warnings)
        case Warning(warnings, _)      => Some(Chunk.empty, warnings)
        case Success(_)                => Some(Chunk.empty, Chunk.empty)
      }
  }

  object WarningsAndValue {
    def unapply[E, A](validated: Validation[E, A]): Option[(Chunk[E], A)] =
      validated match {
        case Warning(warnings, value) => Some(warnings, value)
        case Success(value)           => Some(Chunk.empty, value)
        case Failure(_, _)            => None
      }
  }
}
