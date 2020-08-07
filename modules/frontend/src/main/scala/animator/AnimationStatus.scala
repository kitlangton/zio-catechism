package animator

sealed trait AnimationStatus[+A] {
  def value: A
  def isDone: Boolean      = false
  def isAnimating: Boolean = false
  def map[B: Animatable](f: A => B): AnimationStatus[B]
}

object AnimationStatus {
  case class Animating[A: Animatable](value: A) extends AnimationStatus[A] {
    override def isAnimating: Boolean = true

    override def map[B: Animatable](f: A => B): AnimationStatus[B] =
      Animating(f(value))
  }

  case class Done[A: Animatable](value: A) extends AnimationStatus[A] {
    override def isDone: Boolean = true

    override def map[B: Animatable](f: A => B): AnimationStatus[B] =
      Done(f(value))
  }
}
