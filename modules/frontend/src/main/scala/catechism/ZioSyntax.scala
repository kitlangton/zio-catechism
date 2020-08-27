package catechism

import zio.{CancelableFuture, ZEnv, ZIO}

object ZioSyntax {
  implicit final class ZioOps[A](val z: ZIO[ZEnv, Throwable, A]) extends AnyVal {
    def runAsync: CancelableFuture[A] = zio.Runtime.default.unsafeRunToFuture(z)
  }
}
