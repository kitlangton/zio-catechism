package com.raquo.laminar.ext


import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.nodes.ReactiveHtmlElement

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

trait RepeatWithIntervalHelper {

  def repeatWithInterval[T](
                             value: T,
                             interval: FiniteDuration,
                             observer: Observer[T]
                           ): BinderWithStartStop[ReactiveHtmlElement.Base] = repeatWithInterval(value, interval, t => observer.onNext(t))

  def repeatWithInterval[T](
                             value: T,
                             interval: FiniteDuration,
                             onNext: T => Unit
                           ): BinderWithStartStop[ReactiveHtmlElement.Base] = new BinderWithStartStop[ReactiveHtmlElement.Base] {

    private var maybeTimer: Option[SetIntervalHandle] = Option.empty

    def doStop(): Unit = {
      maybeTimer.foreach { timer =>
        js.timers.clearInterval(timer)
        maybeTimer = Option.empty
      }
    }

    def doStart(): Unit = {
      if (maybeTimer.isEmpty) {
        maybeTimer = Some(
          js.timers.setInterval(interval) {
            onNext(value)
          }
        )
      }
    }

  }

  def repeatWithInterval[T](
                             value: T,
                             interval: FiniteDuration,
                           ): RepeatWithIntervalReceiver[T] = new RepeatWithIntervalReceiver(value, interval)

  class RepeatWithIntervalReceiver[T](
                                       value: T,
                                       interval: FiniteDuration,
                                     ) {

    @inline def -->(observer: Observer[T]): BinderWithStartStop[ReactiveHtmlElement.Base] = {
      repeatWithInterval(value, interval, t => observer.onNext(t))
    }

    @inline def -->(onNext: T => Unit): BinderWithStartStop[ReactiveHtmlElement.Base] = {
      repeatWithInterval(value, interval, onNext)
    }

    @inline def -->(eventBus: EventBus[T]): BinderWithStartStop[ReactiveHtmlElement.Base] = {
      repeatWithInterval(value, interval, t => eventBus.writer.onNext(t))
    }

  }
}
