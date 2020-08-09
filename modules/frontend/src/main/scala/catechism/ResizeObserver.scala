package catechism

import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalajs.dom.ClientRect

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

//@js.native
//trait IntersectionObserverEntry extends js.Object {
//  def isIntersecting: Boolean = js.native
//}
//
//@js.native
//@JSGlobal
//class IntersectionObserver(callback: js.Function1[js.Array[IntersectionObserverEntry], Unit]) extends js.Object {
//  def observe(element: dom.Element): Unit = js.native
//}
//
//object IntersectionObserver {
//  def intersection(element: Element): EventStream[Boolean] = {
//    val bus = new EventBus[Boolean]
//    val observer = new IntersectionObserver({ e =>
//      val isIntersecting = e.headOption.exists(_.isIntersecting)
//      bus.writer.onNext(isIntersecting)
//    })
//    observer.observe(element.ref)
//    bus.events
//  }
//}

@js.native
trait ResizeObserverEntry extends js.Object {
  def contentRect: ClientRect = js.native
}

@js.native
@JSGlobal
class ResizeObserver(callback: js.Function1[js.Array[ResizeObserverEntry], Unit]) extends js.Object {
  def observe(element: dom.Element): Unit = js.native
}

object ResizeObserver {
  def resize(element: Element): EventStream[ClientRect] = {
    val bus = new EventBus[ClientRect]
    val observer = new ResizeObserver({ e =>
      e.headOption.foreach(e => bus.writer.onNext(e.contentRect))
    })
    observer.observe(element.ref)
    bus.events
  }
}
