package formula

import blogus.markdown.MarkdownParser.CustomMarkdownStringContext
import catechism.ObservableSyntax.ObservableOps
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import formula.Form.{FormulaImpl, validation}
import magnolia._
import org.scalajs.dom.html
import zio.Chunk
import Validation._

import com.raquo.laminar.api.L

import scala.language.experimental.macros

sealed trait Form[A] { self =>
  import Form._

  def validate(predicate: A => Boolean, message: String): Form[A] = Form.Validated(self, predicate, message)
  def ~[B](that: Form[B]): Form[(A, B)]                           = Form.Both(self, that)

  /**
    * Say you have form that's just a text input `Form[String]` and you want to _map_ that form into a type of
    * `Form[Int]`, there's a chance that such a transformation could fail. So you need a `String => Attempt[Int]`.
    */
  def exmap[B](from: A => Validation[String, B])(to: B => A): Form[B] = Form.Map(self, from, to)
  def xmap[B](from: A => B)(to: B => A): Form[B]                      = exmap(a => Success(from(a)))(to)

  def named(name: String): Form[A] = self match {
    case input: Input            => input.copy(name = name).asInstanceOf[Form[A]]
    case input: Checkbox         => input.copy(name = name).asInstanceOf[Form[A]]
    case map: Map[_, _]          => map.copy(form = map.form.named(name))
    case validated: Validated[_] => validated.copy(form = validated.form.named(name))
    case both: Both[_, _]        => both
  }

  def render(events: EventStream[Validation[String, A]]): FormulaImpl[A]
}

object Form extends TupleSyntax {
  def string(name: String): Form[String] = Input(name)
  def int(name: String): Form[Int] =
    Input(name).exmap(_.toIntOption.fold[Validation[String, Int]](Failure(Chunk("Not a valid int")))(int =>
      Success(int)))(_.toString)
  def boolean(name: String): Form[Boolean] = Checkbox(name)

  private[formula] case class Checkbox(name: String) extends Form[Boolean] {
    override def render(events: L.EventStream[Validation[String, Boolean]]): FormulaImpl[Boolean] =
      FormViews.checkboxFormula(name, events)
  }

  private[formula] case class Input(name: String) extends Form[String] {
    override def render(events: L.EventStream[Validation[String, String]]): FormulaImpl[String] =
      FormViews.textFormula(name, events)
  }

  private[formula] case class Validated[A](form: Form[A], predicate: A => Boolean, message: String) extends Form[A] {
    override def render(events: L.EventStream[Validation[String, A]]): FormulaImpl[A] =
      form
        .render(events)
        .mapSignal(_.map {
          case Success(a) if !predicate(a) => Validation.warn(message, a)
          case other                       => other
        })
  }

  private[formula] case class Both[A, B](left: Form[A], right: Form[B]) extends Form[(A, B)] {
    override def render(events: L.EventStream[Validation[String, (A, B)]]): FormulaImpl[(A, B)] = {
      val leftFormula  = left.render(events.map(_.map(_._1)))
      val rightFormula = right.render(events.map(_.map(_._2)))
      FormulaImpl(
        leftFormula.signal.combineWith(rightFormula.signal).map {
          case (left, right) =>
            left <&> right
        },
        div(
          leftFormula.render,
          rightFormula.render
        )
      )
    }
  }

  private[formula] case class Map[A, B](form: Form[A], from: A => Validation[String, B], to: B => A) extends Form[B] {
    override def render(events: L.EventStream[Validation[String, B]]): FormulaImpl[B] =
      form
        .render(events.map(_.map(to)))
        .mapSignal(_.map(_.flatMap(from)))
  }

  implicit final class FormOpsTuple2[A, B](val form: Form[(A, B)]) extends AnyVal {
    def mapN[C](f: (A, B) => C)(g: C => Option[(A, B)]): Form[C] =
      form.xmap[C](f)(g.andThen(_.get))
  }

  implicit final class FormOpsTuple3[A, B, C](val form: Form[((A, B), C)]) extends AnyVal {
    def mapN[D](f: (A, B, C) => D)(g: D => Option[(A, B, C)]): Form[D] =
      form.xmap[D](f)(g.andThen(_.get match {
        case (a, b, c) => ((a, b), c)
      }))
  }

  private[formula] case class FormulaImpl[A](signal: Signal[Validation[String, A]], render: ReactiveHtmlElement.Base) {
    def mapElement(f: ReactiveHtmlElement.Base => ReactiveHtmlElement.Base): FormulaImpl[A] =
      copy(render = f(render))

    def mapSignal[B](f: Signal[Validation[String, A]] => Signal[Validation[String, B]]): FormulaImpl[B] =
      copy(signal = f(signal))
  }

  private[formula] case class Formula[A](signal: Signal[Validation[String, A]],
                                         writer: Observer[Validation[String, A]],
                                         render: ReactiveHtmlElement.Base)

  def formula[A](implicit form: Form[A]): Formula[A] = {
    val eventBus = new EventBus[Validation[String, A]]

    val formula = form.render(eventBus.events)

    Formula(
      formula.signal,
      eventBus.writer,
      formula.render
    )
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
        (head ~ collectAll(tHead, tail.tail)).xmap({ case (a, value) => a :: value })(b => b.head -> b.tail)
      case None =>
        head.xmap(List(_))(_.head)
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
            val form     = param.typeclass.asInstanceOf[Form[Any]].named(named)

            validate match {
              case Some((predicate, msg)) => form.validate(predicate.asInstanceOf[Any => Boolean], msg)
              case None                   => form
            }
          }

          collectAll(makeForm(head), tail.map(makeForm))
            .xmap(l => caseClass.rawConstruct(l))(t => caseClass.parameters.map(_.dereference(t)).toList)
      }

    res
  }

  implicit def gen[T]: Form[T] = macro Magnolia.gen[T]

  def mkForm[T](implicit form: Form[T]): Form[T] =
    form

}

object FormViews {
  def checkboxFormula(name: String, events: EventStream[Validation[String, Boolean]]): FormulaImpl[Boolean] = {
    val output: Var[Validation[String, Boolean]] = Var(Validation.succeed(false))

    val inputWriter = new EventBus[Boolean]

    val render = div(
      margin := "12px",
      div(
        label(name)
      ),
      input(
        inputWriter.events.map(Success(_)) --> output.writer,
        typ := "checkbox",
        events --> output.writer,
        checked <-- events.collect { case Success(value) => value },
        inContext { el: ReactiveHtmlElement[html.Input] =>
          onInput.mapTo(Success(el.ref.checked)) --> output.writer
        }
      )
    )

    FormulaImpl(
      output.signal,
      render
    )
  }

  def textFormula(name: String, events: EventStream[Validation[String, String]]): FormulaImpl[String] = {
    val output: Var[Validation[String, String]] = Var(Validation.succeed(""))

    val inputWriter = new EventBus[String]

    val render = div(
      margin := "12px",
      div(
        label(name)
      ),
      input(
        placeholder := name,
        value <-- events.collect { case Success(value) => value },
        inputWriter.events.map(Success(_)) --> output.writer,
        events --> output.writer,
        inContext { el =>
          onInput.mapTo(el.ref.value) --> inputWriter.writer
        }
      )
    )

    FormulaImpl(
      output.signal,
      render
    )
  }
}

object Example {
  import Form._
  case class Person(
      name: String,
      @validation((a: Int) => a >= 18, "Must be 18 or older")
      age: Int,
      @validation((a: Boolean) => a, "Must not be dead")
      isAlive: Boolean
  )

  case class Dog(name: String, likesCats: Boolean = true)

  val specialForm = (Form.string("Name") ~ Form.int("Age") ~ Form.boolean("Is Alive"))
    .mapN(Person.apply)(Person.unapply)

  lazy val formula = Form.formula[Person]

  lazy val body = div(
    md"## Voter Registration",
    button(
      "Fail",
      onClick.mapTo(Success(Person("Kit", 12, false))) --> formula.writer
    ),
    button(
      "Yay",
      onClick.mapTo(Success(Person("Bill", 38, true))) --> formula.writer
    ),
    formula.render,
    pre(
      div(
        "RESULTS"
      ),
      child.text <-- formula.signal.string
    )
  )
}
