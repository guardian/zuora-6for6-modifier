package com.gu.zuora6for6modifier

import java.io.File

import com.gu.zuora6for6modifier.Subscription.extractDataForExtending
import zio.console.{Console, putStrLn}
import zio.{RIO, URIO, ZIO}

object Extender {

  def extendSubscription(subscriptionName: String): URIO[Zuora with Console, Unit] =
    Zuora.>.getSubscription(subscriptionName)
      .flatMap(sub => ZIO.fromEither(extractDataForExtending(subscriptionName, sub)))
      .flatMap(subData => Zuora.>.extendSubscription(subData))
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )

  val extendSubscriptions: RIO[Zuora with Console, Unit] =
    for {
      subNames <- FileHandler.subscriptionNames(new File("extend.in.txt"))
      _ <- ZIO.foreach(subNames)(extendSubscription)
    } yield ()
}
