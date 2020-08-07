package animator

import Animator._
import scala.collection.mutable

trait AnimatableInstances {

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

}
