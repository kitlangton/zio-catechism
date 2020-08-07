package animator

import scala.language.experimental.macros
import magnolia._

import scala.collection.mutable

trait Animatable[A] {
  def size: Int

  def toAnimations(value: A): mutable.IndexedSeq[Double]

  def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): A
}

// Magnolia derivation for Case Classes
object Animatable {
  implicit val stringAnimatable: Animatable[String] = new Animatable[String] {
    override def size: Int = 1

    override def toAnimations(value: String): mutable.IndexedSeq[Double] =
      mutable.IndexedSeq.from(value.toCharArray.map(_.toDouble))

    override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): String =
      animations.map(_.value.toInt.toChar).mkString("")

  }

  implicit val doubleAnimatable: Animatable[Double] = new Animatable[Double] {
    override def size: Int = 1

    override def toAnimations(value: Double): mutable.IndexedSeq[Double] =
      mutable.IndexedSeq(value)

    override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): Double =
      animations.head.value
  }

  implicit def tupleAnimatable[A, B](implicit animA: Animatable[A], animB: Animatable[B]): Animatable[(A, B)] =
    new Animatable[(A, B)] {
      override def size: Int = animA.size + animB.size

      override def toAnimations(value: (A, B)): mutable.IndexedSeq[Double] =
        animA
          .toAnimations(value._1)
          .appendedAll(animB.toAnimations(value._2))

      override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): (A, B) =
        animA.fromAnimations(animations.take(animA.size)) -> animB
          .fromAnimations(animations.drop(animA.size))
    }

  implicit def tuple3Animatable[A, B, C](implicit animA: Animatable[A],
                                         animB: Animatable[B],
                                         animC: Animatable[C]): Animatable[(A, B, C)] = new Animatable[(A, B, C)] {
    override def size: Int = animA.size + animB.size + animC.size

    override def toAnimations(value: (A, B, C)): mutable.IndexedSeq[Double] =
      animA
        .toAnimations(value._1)
        .appendedAll(animB.toAnimations(value._2))
        .appendedAll(animC.toAnimations(value._3))

    override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): (A, B, C) =
      (animA.fromAnimations(animations.take(animA.size)),
       animB.fromAnimations(animations.slice(animA.size, animA.size + animB.size)),
       animC.fromAnimations(animations.drop(animA.size + animB.size)))
  }

  implicit def tuple4Animatable[A, B, C, D](
      implicit animA: Animatable[A],
      animB: Animatable[B],
      animC: Animatable[C],
      animD: Animatable[D],
  ): Animatable[(A, B, C, D)] = new Animatable[(A, B, C, D)] {
    override def size: Int = animA.size + animB.size + animC.size

    override def toAnimations(value: (A, B, C, D)): mutable.IndexedSeq[Double] =
      animA
        .toAnimations(value._1)
        .appendedAll(animB.toAnimations(value._2))
        .appendedAll(animC.toAnimations(value._3))
        .appendedAll(animD.toAnimations(value._4))

    override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): (A, B, C, D) =
      (animA.fromAnimations(animations.take(animA.size)),
       animB.fromAnimations(animations.slice(animA.size, animA.size + animB.size)),
       animC.fromAnimations(animations.slice(animB.size + animB.size, animA.size + animB.size + animC.size)),
       animD.fromAnimations(animations.drop(animA.size + animB.size + animC.size)))
  }

  // Magnolia Derivation

  type Typeclass[T] = Animatable[T]

  def combine[T](ctx: CaseClass[Animatable, T]): Animatable[T] =
    new Animatable[T] {
      override def size: Int = ctx.parameters.length

      override def toAnimations(value: T): mutable.IndexedSeq[Double] = {
        val seq = mutable.IndexedSeq.newBuilder[Double]
        ctx.parameters.foreach { param =>
          seq.addAll(
            param.typeclass.toAnimations(param.dereference(value))
          )
        }
        seq.result()
      }

      override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): T = {
        var index = 0
        ctx.construct { param =>
          val animatable = param.typeclass
          val value      = animatable.fromAnimations(animations.slice(index, animatable.size))
          index += animatable.size
          value
        }
      }
    }

  implicit def gen[T]: Animatable[T] = macro Magnolia.gen[T]
}
