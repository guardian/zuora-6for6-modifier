package com.gu.zuora6for6modifier

import java.io.File

import zio.ZIO

import scala.io.Source

object FileHandler {

  def subscriptionNames(src: File): ZIO[Any, Throwable, List[String]] = {
    val open = ZIO.effect(Source.fromFile(src))
    open.bracketAuto { src =>
      ZIO.effect(src.getLines().toList)
    }
  }
}
