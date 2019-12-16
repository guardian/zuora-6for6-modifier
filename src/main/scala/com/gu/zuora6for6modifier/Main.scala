package com.gu.zuora6for6modifier

import zio.console.{Console, putStrLn}
import zio.{App, ZEnv, ZIO}

import scala.io.Source

object Main extends App {

  val src = "in.csv"

  val subNamesListed: ZIO[Any, Throwable, List[String]] = {
    val open = ZIO.effect(Source.fromFile(src))
    open.bracketAuto { src =>
      ZIO.effect(src.getLines().toList)
    }
  }

  def extractData(subName: String, subscription: String): ZIO[Any, String, SubData] =
    ZIO.fromEither(Subscription.extractData(subName, subscription))

  def processSub(
      accessToken: String,
      subName: String
  ): ZIO[Zuora with Console, Nothing, Unit] =
    Zuora.>.getSubscription(accessToken, subName)
      .flatMap(sub => extractData(subName, sub).mapError(e => new RuntimeException(e)))
      .flatMap(subData => Zuora.>.putSubscription(accessToken, subData))
      .either
      .flatMap {
        case Left(e)  => putStrLn(s"$subName\t\tFAIL\t\t${e.getMessage}")
        case Right(_) => putStrLn(s"$subName\t\tSUCCESS")
      }

  val processSubs: ZIO[Zuora with Console, Any, Unit] =
    for {
      subNames <- subNamesListed
      accessToken <- Zuora.>.token.tapError(e => putStrLn(e.getMessage))
      _ <- ZIO.foreach(subNames)(processSub(accessToken, _))
    } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    processSubs.provide(new ZuoraLive with Console.Live).fold(_ => 1, _ => 0)
}
