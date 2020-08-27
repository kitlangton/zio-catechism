package catechism

import catechism.ObservableSyntax._
import com.raquo.laminar.api.L._

object Pager {
  lazy val currentPageNumber = Var(0)

  lazy val main =
    div(
      windowEvents.onKeyPress.map(_.key.toInt) --> currentPageNumber.writer,
      margin := "0 auto",
      maxWidth := "650px",
      h1("Hello"),
      child.text <-- currentPageNumber.signal.string,
      $currentPage
    )

  lazy val $currentPage2 = Transitions.transitioning(currentPageNumber.signal)(identity) {
    (key, init, $signal, $status) =>
      val $opacity = $status.map {
        case TransitionStatus.Active => 1.0
        case _                       => 0.0
      }.spring

      div(
        opacity <-- $opacity,
        overflowY.hidden,
        width := "650px",
        margin := "0",
        padding := "0",
        init match {
          case 0 => page0
          case 1 => page1
          case 2 => page2
          case _ => page3
        },
        onMountBind { ref =>
          maxHeight <-- $status.map {
            case TransitionStatus.Active => ref.thisNode.ref.parentElement.scrollWidth.toDouble
            case _                       => 0.0
          }.spring.px
        },
      )
  }

  lazy val $currentPage = Transitions.slide(currentPageNumber.signal.map(Seq(_)))(identity) { (a, _) =>
    a match {
      case 0 => page0
      case 1 => page1
      case 2 => page2
      case _ => page3
    }
  }

  def page0 = ZioCatechism.collectAllLesson.render
  def page1 = ZioCatechism.collectAllParLesson.render
  def page2 = ZioCatechism.forkLesson.render
  def page3 = ZioCatechism.joinLesson.render
}
