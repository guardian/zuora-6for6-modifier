package com.gu.zuora6for6modifier

import zio.Task

import java.io.File
import scala.io.Source

object FileHandler {

  def subscriptionNames(src: File): Task[List[String]] = {
    val open = Task.effect(Source.fromFile(src))
    open.bracketAuto { src =>
      Task.effect(src.getLines().toList)
    }
  }
}
