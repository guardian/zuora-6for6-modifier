package com.gu.zuora6for6modifier

import java.io.File

import com.gu.zuora6for6modifier.Subscription.extractDataForPostponing
import zio.console.{Console, putStrLn}
import zio.{RIO, URIO, ZIO}

object Postponer {

  def postponeSubscription(subscriptionName: String): URIO[Zuora with Console, Unit] =
    Zuora
      .getSubscription(subscriptionName)
      .flatMap(sub => ZIO.fromEither(extractDataForPostponing(subscriptionName, sub)))
      .flatMap(subData => Zuora.postponeSubscription(subData))
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )

  val postponeSubscriptions: RIO[Zuora with Console, Unit] =
    for {
      subNames <- FileHandler.subscriptionNames(new File("postpone.in.txt"))
      _ <- ZIO.foreach_(subNames)(postponeSubscription)
    } yield ()
}
