package animator

sealed trait Easing { self =>
  def apply(t: Double): Double
  def inOut: Easing = new Easing {
    override def apply(t: Double): Double =
      if (t < 0.5) self(t * 2) / 2 else 1 - self(t * -2 + 2) / 2
  }
  def out: Easing = new Easing {
    override def apply(t: Double): Double =
      1.0 - self(1.0 - t)
  }
}

object Easing {
  def linear: Easing = new Easing {
    override def apply(t: Double): Double = t
  }

  def sine: Easing = new Easing {
    override def apply(t: Double): Double = 1 - Math.cos(t * Math.PI / 2)
  }

  def circ: Easing = new Easing {
    override def apply(t: Double): Double = 1 - Math.sqrt(1 - t * t)
  }

  def quad: Easing = new Easing {
    override def apply(t: Double): Double = Math.pow(t, 2)
  }

  def cubic: Easing = new Easing {
    override def apply(t: Double): Double = Math.pow(t, 3)
  }

  def quart: Easing = new Easing {
    override def apply(t: Double): Double = Math.pow(t, 4)
  }

  def quint: Easing = new Easing {
    override def apply(t: Double): Double = Math.pow(t, 5)
  }

  def expo: Easing = new Easing {
    override def apply(t: Double): Double = Math.pow(t, 6)
  }

  def back: Easing = new Easing {
    override def apply(t: Double): Double = {
      val c1 = 1.70158;
      val c3 = c1 + 1;

      c3 * t * t * t - c1 * t * t;
    }
  }

  def elastic: Easing = new Easing {
    override def apply(t: Double): Double = {
      val c4 = (2 * Math.PI) / 3;

      if (t == 0)
        0
      else if (t == 1)
        1
      else -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4);
    }
  }

  def bounce: Easing = new Easing {
    override def apply(t: Double): Double = {
      var pow2, b = 4.0;
      while (t < (Math.pow(2, b) - 1) / 11) {
        b = b - 1
        pow2 = Math.pow(2, b)
      };
      1 / Math.pow(4, 3 - b) - 7.5625 * Math.pow((pow2 * 3 - 2) / 22 - t, 2)
    }
  }
}
