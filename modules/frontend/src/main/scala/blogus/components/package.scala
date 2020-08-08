package blogus

import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._
import com.raquo.laminar.ext.CSS._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.{Paragraph, Pre}
import catechism.Highlight

import scala.util.Random

package object components {

  def quote(quoted: String, quotee: Option[String] = Some("Edsger W. Dijkstra")) = {
    blockQuote(
      color := "rgba(255,255,255,0.6)",
      fontStyle.italic,
      padding := "0 12px",
      div(
        quoted.mkString("”", "", "”")
      ),
      quotee.map { quotee =>
        div(
          textAlign.right,
          fontVariant := "small-caps",
          s"— $quotee"
        )
      }
    )
  }

  def k_p(
      modifiers: Modifier[ReactiveHtmlElement[_ <: org.scalajs.dom.html.Element]]*): ReactiveHtmlElement[Paragraph] = p(
    modifiers,
  )

  def subtext(modifiers: Modifier[ReactiveHtmlElement[_ <: org.scalajs.dom.html.Element]]*) = div(
    fontSize := "18px",
    opacity := "0.6",
    modifiers
  )

  private lazy val exampleScalaCode =
    """val postTitles = Seq(
      |  PostTitle("At the Gates of Logic", "Boolean functions and more."),
      |  PostTitle("Animation Indulgence", "How far is too far?"),
      |  PostTitle("Functional Programming", "How a monad is more like a tamale."),
      |  PostTitle("Easings Demo", "", Some(() => Routes.pushState(SpringExample))),
      |  PostTitle("Keyframes Demo", "", Some(() => Routes.pushState(KeyframesExample))),
      |)
      |""".stripMargin

  def codeBlock(codeString: String = exampleScalaCode,
                language: String = "scala",
                codeSignal: Option[Signal[String]] = None,
                marginBottom: Boolean = true): ReactiveHtmlElement[Pre] =
    pre(
      cls := "code",
      borderRadius := "4px",
      margin.maybe(Option.when(!marginBottom)("0px")),
      fontSize := "14px",
      overflowX.scroll,
      padding := "10px",
      code(
        onMountCallback { el =>
          val result = Highlight.highlight(language, codeString).value
          el.thisNode.ref.innerHTML = result
        },
        codeSignal.map { signal =>
          inContext { (el: HtmlElement) =>
            signal --> { str =>
              val result = Highlight.highlight(language, str).value
              el.ref.innerHTML = result
            }
          }
        }
      )
    )

  private lazy val loremIpsumText =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
      .split(" ")

  def loremIpsum(length: Int = 60) =
    k_p(
      List.fill(length)(loremIpsumText(Random.nextInt(loremIpsumText.length))).mkString(" ").capitalize + "."
    )
}
