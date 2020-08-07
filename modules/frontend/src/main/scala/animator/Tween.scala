package animator

sealed trait Tween {
  def value: Double
  def setStart(newValue: Double, t: Double): Tween
  def tick(t: Double): Tween
  def isDone: Boolean
}

object Tween {

  case class Eased(var value: Double = 0,
                   delay: Double = 0,
                   startValue: Double = 0,
                   var target: Double = 0,
                   duration: Double = 600,
                   startTime: Double = 0,
                   easing: Easing = Easing.circ.inOut,
                   var isDone: Boolean = false)
      extends Tween {
    override def tick(t: Double): Eased = {
      val actualStartTime = startTime + delay
      val elapsed         = minMax(t - actualStartTime, 0, duration)
      val percentComplete = Math.min(1.0, elapsed / duration)
      val eased           = easing(percentComplete)
      val delta           = target - startValue
      val nextValue       = startValue + (eased * delta)

      this.value = nextValue
      this.isDone = elapsed >= duration
      this
    }

    override def setStart(newValue: Double, t: Double): Tween =
      copy(value = newValue, startValue = newValue, startTime = t)

    def setTarget(newTarget: Double) = {
      this.target = newTarget
    }
  }

  object Eased {
    def fromValue(value: Double, t: Double): Eased =
      Eased(value = value, startValue = value, target = value, startTime = t)
  }

  final case class Spring(var value: Double = 0,
                          speedFactor: Double = 1.0,
                          var velocity: Double = 0,
                          var target: Double = 0,
                          var oldTarget: Double = 0,
                          stiffness: Double = 170,
                          damping: Double = 26,
                          precision: Double = 0.005,
                          var lastTime: Option[Double] = None)
      extends Tween {
    self =>
    def tick(t: Double): Spring = {
      val delta = 1.0 / 80.0 * speedFactor

      val fSpring = -stiffness * (value - target);
      val fDamper = -damping * velocity;
      val a       = fSpring + fDamper;

      val newVelocity = velocity + a * delta
      val newValue    = value + newVelocity * delta;

      if (Math.abs(newVelocity) < precision && Math.abs(newValue - target) < precision) {
        value = target
        velocity = 0
        lastTime = Some(t)
        this
      } else {
        value = newValue
        velocity = newVelocity
        lastTime = Some(t)
        if (lastTime.exists(lt => t - lt > 1000 / 40)) tick(t) else this
      }
    }

    def isDone: Boolean = value == target && velocity == 0

    def setTarget(newTarget: Double): Unit = if (newTarget != target) {
      this.target = newTarget
      this.oldTarget = target
    }

    override def setStart(newValue: Double, t: Double): Tween =
      copy(value = newValue)
  }

  object Spring {
    def fromValue(value: Double, t: Double): Spring =
      Spring(value = value, target = value)
  }

  def minMax(value: Double, min: Double, max: Double): Double =
    Math.min(Math.max(value, min), max)
}
