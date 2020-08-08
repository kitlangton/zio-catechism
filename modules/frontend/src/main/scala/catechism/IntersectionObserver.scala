package catechism

import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait IntersectionObserverEntry extends js.Object {
  def isIntersecting: Boolean = js.native
}

@js.native
@JSGlobal
class IntersectionObserver(callback: js.Function1[js.Array[IntersectionObserverEntry], Unit]) extends js.Object {
  def observe(element: dom.Element): Unit = js.native
}

object IntersectionObserver {
  def intersection(element: Element): EventStream[Boolean] = {
    val bus = new EventBus[Boolean]
    val observer = new IntersectionObserver({ e =>
      val isIntersecting = e.headOption.exists(_.isIntersecting)
      bus.writer.onNext(isIntersecting)
    })
    observer.observe(element.ref)
    bus.events
  }
}
