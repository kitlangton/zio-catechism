package formula

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import magnolia._
import org.scalajs.dom.html

import scala.language.experimental.macros

case class LensVar[A](signal: Signal[A], writer: Observer[A], now: () => A) {
  def lens[B](from: A => B)(to: (A, B) => A): LensVar[B] = {
    val signal2: Signal[B]   = signal.map(from)
    val writer2: Observer[B] = writer.contramap[B](to(now(), _))
    LensVar(signal2, writer2, () => from(now()))
  }
}

object LensVar {
  def fromVar[A](variable: Var[A]): LensVar[A] = LensVar(variable.signal, variable.writer, () => variable.now())

  implicit final class VarOps[A](val variable: Var[A]) extends AnyVal {
    def lens: LensVar[A]                                   = LensVar.fromVar(variable)
    def lens[B](from: A => B)(to: (A, B) => A): LensVar[B] = lens.lens(from)(to)
  }
}

sealed trait Form[A] { self =>
  def validate(predicate: A => Boolean, message: String): Form[A] =
    Form.Validated(self, predicate, message)
  def ++[B](that: Form[B]): Form[(A, B)]        = Form.Both(self, that)
  def map[B](from: A => B)(to: B => A): Form[B] = Form.Map(self, from, to)
}

object Form {
  def string(name: String): Form[String]   = Input(name)
  def int(name: String): Form[Int]         = Input(name).map(_.toInt)(_.toString)
  def boolean(name: String): Form[Boolean] = Checkbox(name)

  case class Checkbox(name: String)                                                extends Form[Boolean]
  case class Input(name: String)                                                   extends Form[String]
  case class Group[A](form: Form[A], name: String)                                 extends Form[A]
  case class Validated[A](form: Form[A], predicate: A => Boolean, message: String) extends Form[A]
  case class Both[A, B](left: Form[A], right: Form[B])                             extends Form[(A, B)]
  case class Map[A, B](form: Form[A], from: A => B, to: B => A) extends Form[B] {
    def mapLens(lensVar: LensVar[B]): LensVar[A] = {
      lensVar.lens(to)((a, b) => from(b))
    }
  }

  implicit final class FormOps[F](val form: Form[F]) extends AnyVal {
    def as[A, B](from: A => B, to: B => F)(implicit ev: F <:< A): Form[B] = form.map(ev.andThen(from))(to(_))
    def as[A, B, C](from: (A, B) => C, to: C => (A, B))(implicit ev: F <:< (A, B), ev1: (A, B) <:< F): Form[C] =
      form.map[C](ev.andThen { case (a, b) => from(a, b) })(value => ev1(to(value)))
    def as[A, B, C, D](from: (A, B, C) => D, to: D => Option[(A, B, C)])(implicit ev: F <:< ((A, B), C),
                                                                         ev1: ((A, B), C) <:< F): Form[D] =
      form.map(ev.andThen { case ((a, b), c) => from(a, b, c) })((value: D) =>
        to(value) match {
          case Some((a, b, c)) => ((a, b), c)
      })

    def named(name: String): Form[F] = form match {
      case input: Input            => input.copy(name = name).asInstanceOf[Form[F]]
      case input: Checkbox         => input.copy(name = name).asInstanceOf[Form[F]]
      case input: Group[F]         => input.copy(name = name).asInstanceOf[Form[F]]
      case map: Map[_, _]          => map.copy(form = map.form.named(name))
      case validated: Validated[_] => validated.copy(form = validated.form.named(name))
      case other                   => other
    }

    def group(name: String): Form[F] = Form.Group(form, name)
  }

  def render[A](variable: Var[A])(implicit form: Form[A]): ReactiveHtmlElement[html.Div] =
    render(form, LensVar.fromVar(variable))

  def render[A](form: Form[A], variable: Var[A]): ReactiveHtmlElement[html.Div] =
    render(form, LensVar.fromVar(variable))

  def render[A](form: Form[A], lensVar: LensVar[A]): ReactiveHtmlElement[html.Div] = form match {
    case Checkbox(name) => FormViews.checkboxInput(name, lensVar)
    case Input(name)    => FormViews.textInput(name, lensVar)
    case Validated(form, predicate, message) =>
      div(
        child.maybe <-- lensVar.signal.map { value =>
          Option.when(!predicate(value)) {
            div(message)
          }
        },
        render(form, lensVar)
      )
    case Both(left, right) =>
      div(
        render(left, lensVar.lens(_._1)((a, b) => (b, a._2))),
        render(right, lensVar.lens(_._2)((a, b) => (a._1, b))),
      )
    case Group(form, name) =>
      div(
        name,
        render(form, lensVar)
      )
    case map: Map[_, A] =>
      render(map.form, map.mapLens(lensVar))
  }

  // Implicits

  implicit val stringForm: Form[String] = Form.string("")
  implicit val intForm: Form[Int]       = Form.int("")
  implicit val boolForm: Form[Boolean]  = Form.boolean("")

  import scala.annotation.StaticAnnotation

  final case class name(name: String)                                      extends StaticAnnotation
  final case class validation[A](predicate: A => Boolean, message: String) extends StaticAnnotation

  type Typeclass[T] = Form[T]

  def collectAll[A](
      head: Form[A],
      tail: List[Form[A]]
  ): Form[List[A]] = {
    tail.headOption match {
      case Some(tHead) =>
        (head ++ collectAll(tHead, tail.tail)).map({ case (a, value) => a :: value })(b => b.head -> b.tail)
      case None =>
        head.map(List(_))(_.head)
    }
  }

  def combine[T](caseClass: CaseClass[Form, T]): Form[T] = {
    val res =
      caseClass.parameters.toList match {
        case Nil =>
          ???
        case head :: tail =>
          def makeForm(param: Param[Form, T]): Form[Any] = {
            val named    = param.annotations.collectFirst { case name(name) => name }.getOrElse(param.label.capitalize)
            val validate = param.annotations.collectFirst { case validation(pred, name) => (pred, name) }
            val form     = param.typeclass.form.asInstanceOf[Form[Any]].named(named)

            validate match {
              case Some((predicate, msg)) => form.validate(predicate.asInstanceOf[Any => Boolean], msg)
              case None                   => form
            }
          }

          Form.Map[List[Any], T](collectAll(makeForm(head), tail.map(makeForm)),
                                 l => caseClass.rawConstruct(l),
                                 t => caseClass.parameters.map(_.dereference(t)).toList)
      }

    res
  }

  implicit def gen[T]: Form[T] = macro Magnolia.gen[T]

  def mkForm[T](implicit form: Form[T]): Form[T] =
    form

}

object FormViews {
  def checkboxInput(name: String, variable: LensVar[Boolean]): ReactiveHtmlElement[html.Div] = div(
    margin := "12px",
    div(
      label(name)
    ),
    input(
      typ := "checkbox",
      placeholder := name,
      checked <-- variable.signal,
      inContext { el =>
        onInput.mapTo(el.ref.checked) --> variable.writer
      }
    )
  )

  def textInput(name: String, variable: LensVar[String]): ReactiveHtmlElement[html.Div] = div(
    margin := "12px",
    div(
      label(name)
    ),
    input(
      placeholder := name,
      value <-- variable.signal,
      inContext { el =>
        onInput.mapTo(el.ref.value) --> variable.writer
      }
    )
  )
}
