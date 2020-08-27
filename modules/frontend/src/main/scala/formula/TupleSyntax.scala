package formula

trait TupleSyntax {
  // Tuple codec syntax

  /**
    * Type alias for Tuple2 in order to allow left nested tuples to be written as A ~ B ~ C ~ ....
    * @group tuples
    */
  final type ~[+A, +B] = (A, B)

  /**
    * Extractor that allows pattern matching on the tuples created by tupling codecs.
    * @group tuples
    */
  object ~ extends Serializable {
    def unapply[A, B](t: (A, B)): Option[(A, B)] = Some(t)
  }

  /**
    * Allows creation of left nested pairs by successive usage of `~` operator.
    * @group tuples
    */
  final implicit class ValueEnrichedWithTuplingSupport[A](val a: A) {
    def ~[B](b: B): (A, B) = (a, b)
  }

  /**
    * Allows use of a 2-arg function as a single arg function that takes a left-associated stack of pairs with 2 total elements.
    * @group tuples
    */
  final implicit def liftF2ToNestedTupleF[A, B, X](fn: (A, B) => X): ((A, B)) => X =
    fn.tupled

  /**
    * Allows use of a 3-arg function as a single arg function that takes a left-associated stack of pairs with 3 total elements.
    * @group tuples
    */
  final implicit def liftF3ToNestedTupleF[A, B, C, X](fn: (A, B, C) => X): (((A, B), C)) => X = {
    case a ~ b ~ c => fn(a, b, c)
  }

  /**
    * Allows use of a 4-arg function as a single arg function that takes a left-associated stack of pairs with 4 total elements.
    * @group tuples
    */
  final implicit def liftF4ToNestedTupleF[A, B, C, D, X](
      fn: (A, B, C, D) => X
  ): ((((A, B), C), D)) => X = {
    case a ~ b ~ c ~ d => fn(a, b, c, d)
  }

  /**
    * Allows use of a 5-arg function as a single arg function that takes a left-associated stack of pairs with 5 total elements.
    * @group tuples
    */
  final implicit def liftF5ToNestedTupleF[A, B, C, D, E, X](
      fn: (A, B, C, D, E) => X
  ): (((((A, B), C), D), E)) => X = {
    case a ~ b ~ c ~ d ~ e => fn(a, b, c, d, e)
  }

  /**
    * Allows use of a 6-arg function as a single arg function that takes a left-associated stack of pairs with 6 total elements.
    * @group tuples
    */
  final implicit def liftF6ToNestedTupleF[A, B, C, D, E, F, X](
      fn: (A, B, C, D, E, F) => X
  ): ((((((A, B), C), D), E), F)) => X = {
    case a ~ b ~ c ~ d ~ e ~ f => fn(a, b, c, d, e, f)
  }

  /**
    * Allows use of a 7-arg function as a single arg function that takes a left-associated stack of pairs with 7 total elements.
    * @group tuples
    */
  final implicit def liftF7ToNestedTupleF[A, B, C, D, E, F, G, X](
      fn: (A, B, C, D, E, F, G) => X
  ): (((((((A, B), C), D), E), F), G)) => X = {
    case a ~ b ~ c ~ d ~ e ~ f ~ g => fn(a, b, c, d, e, f, g)
  }

  /**
    * Allows use of an 8-arg function as a single arg function that takes a left-associated stack of pairs with 8 total elements.
    * @group tuples
    */
  final implicit def liftF8ToNestedTupleF[A, B, C, D, E, F, G, H, X](
      fn: (A, B, C, D, E, F, G, H) => X
  ): ((((((((A, B), C), D), E), F), G), H)) => X = {
    case a ~ b ~ c ~ d ~ e ~ f ~ g ~ h => fn(a, b, c, d, e, f, g, h)
  }

}
