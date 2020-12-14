package com.gu.zuora6for6modifier

import com.gu.zuora6for6modifier.Subscription.{extractDataForPostponing, extractDataForPostponingMainRatePlan}
import zio.console.{Console, putStrLn}
import zio.{RIO, URIO, ZIO}

import java.io.File

object Postponer {

  private def postponePlans(postponePlan: String => URIO[Zuora with Console, Unit]): RIO[Zuora with Console, Unit] =
    for {
      subNames <- FileHandler.subscriptionNames(new File("postpone.in.txt"))
      _ <- ZIO.foreach_(subNames)(postponePlan)
    } yield ()

  val postpone6For6RatePlans: RIO[Zuora with Console, Unit] =
    postponePlans(postpone6For6RatePlan)

  private def postpone6For6RatePlan(subscriptionName: String): URIO[Zuora with Console, Unit] =
    Zuora
      .getSubscription(subscriptionName)
      .flatMap(sub => ZIO.fromEither(extractDataForPostponing(subscriptionName, sub)))
      .flatMap(subData => Zuora.postpone6For6RatePlan(subData))
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )

  val postponeMainRatePlans: RIO[Zuora with Console, Unit] =
    postponePlans(postponeMainRatePlan)

  private def postponeMainRatePlan(subscriptionName: String): URIO[Zuora with Console, Unit] =
    Zuora
      .getSubscription(subscriptionName)
      .flatMap(sub => ZIO.fromEither(extractDataForPostponingMainRatePlan(subscriptionName, sub)))
      .flatMap(subData => Zuora.postponeMainRatePlan(subData))
      .foldM(
        e => putStrLn(s"$subscriptionName\t\tFAIL\t\t${e.getMessage}"),
        _ => putStrLn(s"$subscriptionName\t\tSUCCESS")
      )
}
