package blogus.markdown

import blogus.components.k_p
import com.raquo.laminar.api.L._
import com.raquo.laminar.ext.CSS.fontVariant
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object MarkdownParser {
  import fastparse._
  import NoWhitespace._
  import MD._
  import MD.Inline._
  import MD.Block._

  sealed trait MD extends Product with Serializable
  object MD {

    case class Document(md: Seq[Block]) extends MD

    // Inline
    sealed trait Inline extends MD
    object Inline {
      case class Text(string: String)            extends Inline
      case class InlineCode(string: String)      extends Inline
      case class Emphasized(contents: Seq[MD])   extends Inline
      case class Strong(contents: Seq[MD])       extends Inline
      case class Link(name: String, url: String) extends Inline
    }

    // Block
    sealed trait Block extends MD
    object Block {
      case class Paragraph(contents: Seq[Inline])                    extends Block
      case class CodeBlock(string: String, language: Option[String]) extends Block
      case class ListItem(contents: Seq[MD])                         extends Block
      case class OrderedList(contents: Seq[ListItem])                extends Block
      case class Heading(contents: Seq[Inline], level: Int)          extends Block
    }
  }

  // Whitespace
  private def newline[_: P]: P[Unit]   = "\n"
  private def blankLine[_: P]: P[Unit] = " ".rep ~ "\n" ~ " ".rep

  // Basic
  private def normalChar[_: P]: P[Unit] =
    P(!("*" | "`" | "\n" | "[") ~ AnyChar)

  private def string[_: P]: P[String] =
    P(normalChar.rep(1).!)

  private def text[_: P]: P[Text] =
    P(string.rep(1, "\n")).map(s => Text(s.mkString(" ")))

  // Inline
  private def inline[_: P]: P[Inline] = (link | strong | emphasized | inlineCode | text)

  private def inlineCode[_: P]: P[InlineCode] =
    P("`" ~ CharsWhile(_ != '`').! ~ "`").map(InlineCode)

  private def emphasized[_: P]: P[Emphasized] =
    P("*" ~ inline.rep(1) ~ "*").map(Emphasized)

  private def strong[_: P]: P[Strong] =
    P("**" ~ inline.rep(1) ~ "**").map(Strong)

  private def link[_: P]: P[Link] =
    P("[" ~ CharsWhile(_ != ']').! ~ "]" ~ "(" ~ CharsWhile(_ != ')').! ~ ")").map(Link.tupled)

  // Block
  private def hash[_: P]: P[Int] = P("#".!.rep(1)).map(_.length)
  private def heading[_: P]: P[Heading] = P(hash ~ " ".? ~ inline.rep(1)).map {
    case (level, contents) => Heading(contents, level)
  }

  private def paragraph[_: P]: P[Paragraph] =
    P(inline.rep(1)).map(Paragraph)

  private def codeText[_: P]: P[Unit] = P(!"```" ~ AnyChar)
  private def codeBlock[_: P]: P[CodeBlock] =
    P(
      "```" ~ string.? ~ "\n" ~ codeText.rep.! ~ "```"
    ).map { case (langOpt, code) => CodeBlock(code, langOpt) }

  private def multiblocks[_: P]: P[Seq[Block]]  = block.rep(1, "\n")
  private def multiInline[_: P]: P[Seq[Inline]] = inline.rep(1)

  //  private def codeText[_: P]: P[Unit] = P(!"```" ~ AnyChar)
  private def orderedPrefix[_: P]: P[Unit] = P(CharIn("0123456789") ~ "." ~ " ".rep(1))
  private def untilNewLine[_: P]           = P(!"\n" ~ AnyChar).rep(1)
  private def listItem[_: P]: P[ListItem] =
    P(" ".!.rep(0).map(_.length).flatMap { indent =>
      orderedPrefix ~ untilNewLine.! ~ ("\n" ~ " ".rep(indent + 3) ~ untilNewLine.!).rep(0)
    }).map {
      case (str, strs) =>
        val line: Seq[Inline] = parse(str, multiInline(_)).get.value

        val nested: Seq[Block] =
          if (strs.isEmpty) Seq.empty
          else parse(strs.mkString("\n"), multiblocks(_)).get.value

        ListItem(line ++ nested)
    }

  private def orderedList[_: P]: P[OrderedList] =
    P(listItem.rep(1, "\n".rep(1))).map(OrderedList)

  private def block[_: P]: P[Block] =
    P(codeBlock | orderedList | heading | paragraph)

  // Markdown
  private def markdown[_: P]: P[MD] =
    P("\n".rep(0) ~ block.rep(1, "\n".rep(1)) ~ "\n".rep(0)).map(Document)

  implicit class CustomMarkdownStringContext(val sc: StringContext) extends AnyVal {
    def md(args: Any*): ReactiveHtmlElement[_ <: html.Element] = {
      println(s"ARGS $args")
      val string = sc.parts.mkString("")
      markdownToElement(string)
    }
  }

  def markdownToElement(string: String): ReactiveHtmlElement[_ <: html.Element] = {
    val parsed = parse(string, markdown(_))
    val md     = parsed.get.value
    div(renderMd(md))
  }

  def renderMd(md: MD): Modifier[ReactiveHtmlElement[_ <: html.Element]] =
    md match {
      case Text(string)       => span(string)
      case InlineCode(string) => blogus.components.inlineCode(string)
      case Emphasized(md)     => span(fontStyle.italic, md.map(renderMd))
      case Strong(md)         => span(fontVariant.smallCaps, md.map(renderMd))
      case Link(name, url)    => a(name, href := url, color := "rgb(80, 144, 255)", fontStyle.italic)

      case Heading(contents, 1) => h1(contents.map(renderMd))
      case Heading(contents, 2) => h2(contents.map(renderMd))
      case Heading(contents, 3) => h3(contents.map(renderMd))
      case Heading(contents, 4) => h4(contents.map(renderMd))
      case Heading(contents, 5) => h5(contents.map(renderMd))
      case Heading(contents, _) => h6(contents.map(renderMd))

      case Paragraph(contents)         => k_p(contents.map(renderMd): _*)
      case CodeBlock(string, language) => blogus.components.codeBlock(string, language.getOrElse("scala"))
      case ListItem(contents)          => contents.map(renderMd)
      case OrderedList(items) =>
        ol(
          items.zipWithIndex.map {
            case (md, i) =>
              li(
                display := "flex",
                div(
                  marginRight := "12px",
                  fontVariant.smallCaps,
                  opacity := "0.6",
                  s"${i + 1}."
                ),
                div(
                  flex := "1",
                  color := "#ccc",
                  renderMd(md)
                )
              )
          }
        )

      case Document(md) => div(md.map(v => renderMd(v)))
    }
}
