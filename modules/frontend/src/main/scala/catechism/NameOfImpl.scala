package catechism

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.annotation.tailrec

trait NameOf {
  import scala.language.experimental.macros

  /**
    * Obtain an identifier name as a constant string.
    *
    * Example usage:
    * {{{
    *   val amount = 5
    *   nameOf(amount) => "amount"
    * }}}
    */
  def nameOf(expr: Any): String = macro NameOfImpl.nameOf

  /**
    * Obtain an identifier name as a constant string.
    *
    * This overload can be used to access an instance method without having an instance of the type.
    *
    * Example usage:
    * {{{
    *   class Person(val name: String)
    *   nameOf[Person](_.name) => "name"
    * }}}
    */
  def nameOf[T](expr: T => Any): String = macro NameOfImpl.nameOf

  /**
    * Obtain a type's unqualified name as a constant string.
    *
    * Example usage:
    * {{{
    *   nameOfType[String] => "String"
    *   nameOfType[fully.qualified.ClassName] => "ClassName"
    * }}}
    */
  def nameOfType[T]: String = macro NameOfImpl.nameOfType[T]

  /**
    * Obtain a type's qualified name as a constant string.
    *
    * Example usage:
    * {{{
    *   nameOfType[String] => "java.lang.String"
    *   nameOfType[fully.qualified.ClassName] => "fully.qualified.ClassName"
    * }}}
    */
  def qualifiedNameOfType[T]: String = macro NameOfImpl.qualifiedNameOfType[T]
}
object NameOf extends NameOf

object NameOfImpl {
  def nameOf(c: blackbox.Context)(expr: c.Expr[Any]): c.Expr[String] = {
    import c.universe._

    @tailrec def extract(tree: c.Tree): c.Name = tree match {
      case Ident(n)           => n
      case Select(_, n)       => n
      case Function(_, body)  => extract(body)
      case Block(_, expr)     => extract(expr)
      case Apply(func, _)     => extract(func)
      case TypeApply(func, _) => extract(func)
      case _                  => c.abort(c.enclosingPosition, s"Unsupported expression: $expr")
    }

    val name = extract(expr.tree).decoded
    reify {
      c.Expr[String] { Literal(Constant(name)) }.splice
    }
  }

  def nameOfType[T](c: blackbox.Context)(implicit tag: c.WeakTypeTag[T]): c.Expr[String] = {
    import c.universe._
    val name = showRaw(tag.tpe.typeSymbol.name)
    reify {
      c.Expr[String] { Literal(Constant(name)) }.splice
    }
  }

  def qualifiedNameOfType[T](c: blackbox.Context)(implicit tag: c.WeakTypeTag[T]): c.Expr[String] = {
    import c.universe._
    val name = showRaw(tag.tpe.typeSymbol.fullName)
    reify {
      c.Expr[String] { Literal(Constant(name)) }.splice
    }
  }
}
