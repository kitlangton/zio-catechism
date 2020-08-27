package object formula {
  import Form._
  def string(name: String): Form[String] = Input(name)
  def int(name: String): Form[Int] =
    Input(name).exmap {
      _.toIntOption match {
        case Some(value) => Validation.succeed(value)
        case None        => Validation.fail("Failed to parse Int")
      }
    } {
      _.toString
    }
  def boolean(name: String): Form[Boolean] = Checkbox(name)
}
