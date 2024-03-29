package animator
import zio._

import scala.collection.mutable

case class Color(red: Double = 0.0, green: Double = 0.0, blue: Double = 0.0, alpha: Double = 1.0) {
  def css = s"rgba($red,$green,$blue,$alpha)"

  def +(other: Color): Color = add(other)

  def add(other: Color): Color =
    Color(red = red + other.red, green = green + other.green, blue = blue + other.blue, alpha = alpha + other.alpha)

  def blend(other: Color): Color =
    Color(red = (red + other.red) / 2.0,
          green = (green + other.green) / 2.0,
          blue = (blue + other.blue) / 2.0,
          alpha = (alpha + other.alpha) / 2.0)
}

object Color {

  def apply(red: Double = 0.0, green: Double = 0.0, blue: Double = 0.0, alpha: Double = 1.0) =
    new Color(red = Math.min(255.0, red),
              green = Math.min(255.0, green),
              blue = Math.min(255.0, blue),
              alpha = Math.min(1.0, alpha))

  lazy val red: Color   = Color(red = 255)
  lazy val blue: Color  = Color(blue = 255)
  lazy val green: Color = Color(green = 255)

  lazy val magenta: Color = Color(red = 255, blue = 255)
  lazy val yellow: Color  = Color(red = 255, green = 255)
  lazy val cyan: Color    = Color(blue = 255, green = 255)

  lazy val black: Color = Color()
  lazy val white: Color = Color(red = 255, blue = 255, green = 255)

  implicit val colorAnimatable: Animatable[Color] = new Animatable[Color] {
    override def size: Int = 4

    override def toAnimations(color: Color): mutable.IndexedSeq[Double] =
      mutable.IndexedSeq(color.red, color.green, color.blue, color.alpha)

    override def fromAnimations(animations: mutable.IndexedSeq[_ <: Tween]): Color =
      Color(animations(0).value, animations(1).value, animations(2).value, animations(3).value)
  }
}
