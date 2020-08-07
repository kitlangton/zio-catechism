package starter

import com.raquo.domtestutils.MountOps
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveElement
import com.raquo.laminar.nodes.RootNode

trait JestMountOps extends MountOps {

  var root: RootNode = _

  def mount(
    node: ReactiveElement.Base
  ): Unit = {
    root = L.render(containerNode, node)
  }

  override def doAssert(condition: Boolean, message: String): Unit = {
    if (!condition) {
      throw new Error(message)
    }
  }

  override def doFail(message: String): Nothing = {
    throw new Error(message)
  }

}
