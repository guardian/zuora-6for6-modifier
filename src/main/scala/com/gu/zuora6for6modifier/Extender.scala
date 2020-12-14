package com.gu.zuora6for6modifier

import java.io.File

import com.gu.zuora6for6modifier.Subscription.extractDataForExtending
import zio.console.{Console, putStrLn}
import zio.{RIO, URIO, ZIO}

object Extender {

  def extend6For6RatePlan(subscriptionName: String): URIO[Zuora with Console, Unit] =
    Zuora
      .getSubscription(subscriptionName)
      .flatMap(sub => ZIO.fromEither(extractDataForExtending(subscriptionName, sub)))
      .flatMap(subData => Zuora.extend6For6RatePlan(subData))
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )

  val extend6For6RatePlans: RIO[Zuora with Console, Unit] =
    for {
      subNames <- FileHandler.subscriptionNames(new File("extend.in.txt"))
      _ <- ZIO.foreach_(subNames)(extend6For6RatePlan)
    } yield ()
}
