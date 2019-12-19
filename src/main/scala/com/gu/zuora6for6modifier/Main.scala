package com.gu.zuora6for6modifier

import java.io.File

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
      extractFrom: (String, String) => Either[Throwable, SubscriptionData]
  ): ZIO[Any, Throwable, SubscriptionData] =
    ZIO.fromEither(extractFrom(subscriptionName, subscription))

  private def processSubscription(
      subscriptionName: String,
      dataRequired: (String, String) => Either[Throwable, SubscriptionData],
      process: SubscriptionData => ZIO[Zuora, Throwable, Unit]
  ): ZIO[Zuora with Console, Nothing, Unit] =
    Zuora.>.getSubscription(subscriptionName)
      .flatMap(sub => extractData(subscriptionName, sub, dataRequired))
      .flatMap(subData => process(subData))
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )

  def extendSubscription(subscriptionName: String): ZIO[Zuora with Console, Nothing, Unit] =
    processSubscription(subscriptionName, extractDataForExtending, Zuora.>.extendSubscription)

  def postponeSubscription(subscriptionName: String): ZIO[Zuora with Console, Nothing, Unit] =
    processSubscription(subscriptionName, extractDataForPostponing, Zuora.>.postponeSubscription)

  val extendSubscriptions: ZIO[Zuora with Console, Throwable, Unit] =
    for {
      subNames <- subscriptionNames(new File("extend.in.txt"))
      _ <- ZIO.foreach(subNames)(extendSubscription)
    } yield ()

  val postponeSubscriptions: ZIO[Zuora with Console, Throwable, Unit] =
    for {
      subNames <- subscriptionNames(new File("postpone.in.txt"))
      _ <- ZIO.foreach(subNames)(postponeSubscription)
    } yield ()

  /**
    * @param args  valid values of arg(0) are:<ul>
    *              <li>"extend" to extend the 6-for-6 period so that it includes at least 6 issues.<br />
    *               In this case a file called "extend.in.txt" will be expected in the root
    *               of the project holding a list of subscription names, one on each line.</li>
    *              <li>"postpone" to postpone the start of the 6-for-6 period by a week<br />
    *               In this case a file called "postpone.in.txt" will be expected in the root
    *               of the project holding a list of subscription names, one on each line.</li></ul>
    *              In both cases the start of the subsequent quarterly plan is postponed
    *              until the new end of the introductory plan.
    */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val action = args.headOption
    val process = action match {
      case Some("extend")   => extendSubscriptions
      case Some("postpone") => postponeSubscriptions
      case _                => ZIO.dieMessage("No identifying action given")
    }
    process
      .provide(new ZuoraLive with Console.Live)
      .foldM(
        e => putStrLn(e.getMessage).map(_ => 1),
        _ => ZIO.succeed(0)
      )
  }
}
