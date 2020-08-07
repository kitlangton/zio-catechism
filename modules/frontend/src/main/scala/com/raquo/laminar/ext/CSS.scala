package com.raquo.laminar.ext
import com.raquo.laminar.builders.HtmlBuilders

object CSS extends HtmlBuilders {
  object gridTemplateColumns
      extends com.raquo.domtypes.generic.keys.Style[String]("gridTemplateColumns", "grid-template-columns")
  object gridTemplateRows
      extends com.raquo.domtypes.generic.keys.Style[String]("gridTemplateRows", "grid-template-rows")

  object gridGap    extends com.raquo.domtypes.generic.keys.Style[String]("gridGap", "grid-gap")
  object gridRow    extends com.raquo.domtypes.generic.keys.Style[String]("gridRow", "grid-row")
  object gridColumn extends com.raquo.domtypes.generic.keys.Style[String]("gridColumn", "grid-column")

  object justifySelf extends com.raquo.domtypes.generic.keys.Style[String]("justifySelf", "justify-self") {
    lazy val end    = buildStringStyleSetter(this, "end")
    lazy val start  = buildStringStyleSetter(this, "start")
    lazy val center = buildStringStyleSetter(this, "center")
  }

  object fontVariant extends com.raquo.domtypes.generic.keys.Style[String]("fontVariant", "font-variant") {
    lazy val smallCaps = buildStringStyleSetter(this, "small-caps")
  }
}
