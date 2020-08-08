package catechism

import zio.{ZEnv, ZIO}

object ZioSyntax {
  implicit final class ZioOps[E, A](val z: ZIO[ZEnv, E, A]) extends AnyVal {
    def runAsync = zio.Runtime.default.unsafeRunAsync_(z)
  }
}
