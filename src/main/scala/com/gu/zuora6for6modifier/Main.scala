package com.gu.zuora6for6modifier

import java.io.File

import com.gu.zuora6for6modifier.PutRequests.{extendTo2Months, startWeekLater}
import com.gu.zuora6for6modifier.Subscription.{extractDataForExtending, extractDataForPostponing}
import zio.console.{Console, putStrLn}
import zio.{App, ZEnv, ZIO}

import scala.io.Source

object Main extends App {

  def subscriptionNames(src: File): ZIO[Any, Throwable, List[String]] = {
    val open = ZIO.effect(Source.fromFile(src))
    open.bracketAuto { src =>
      ZIO.effect(src.getLines().toList)
    }
  }

  def extractData(
      subscriptionName: String,
      subscription: String,
      extractFrom: (String, String) => Either[String, SubscriptionData]
  ): ZIO[Any, String, SubscriptionData] =
    ZIO.fromEither(extractFrom(subscriptionName, subscription))

  def processSubscription(
      accessToken: String,
      subscriptionName: String,
      extractSubscriptionData: (String, String) => Either[String, SubscriptionData],
      putRequestBody: SubscriptionData => String
  ): ZIO[Zuora with Console, Nothing, Unit] =
    Zuora.>.getSubscription(accessToken, subscriptionName)
      .flatMap { sub =>
        extractData(subscriptionName, sub, extractSubscriptionData).mapError(
          e => new RuntimeException(e)
        )
      }
      .flatMap { subData =>
        Zuora.>.putSubscription(accessToken, subData, putRequestBody)
      }
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )

  def processSubs(
      src: File,
      putRequestBody: SubscriptionData => String,
      extractSubData: (String, String) => Either[String, SubscriptionData]
  ): ZIO[Zuora with Console, Throwable, Unit] =
    for {
      subNames <- subscriptionNames(src)
      accessToken <- Zuora.>.token
      _ <- ZIO.foreach(subNames) { subName =>
        processSubscription(accessToken, subName, extractSubData, putRequestBody)
      }
    } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val action = args.headOption
    val process = action match {
      case Some("extend") =>
        processSubs(new File("extend.in.txt"), extendTo2Months, extractDataForExtending)
      case Some("postpone") =>
        processSubs(new File("postpone.in.txt"), startWeekLater, extractDataForPostponing)
      case _ =>
        ZIO.dieMessage("No identifying action or source file given")
    }
    process
      .provide(new ZuoraLive with Console.Live)
      .foldM(
        e => putStrLn(e.getMessage).map(_ => 1),
        _ => ZIO.succeed(0)
      )
  }
}
