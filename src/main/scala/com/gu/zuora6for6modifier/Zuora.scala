package com.gu.zuora6for6modifier

import zio.{RIO, Task}

object Zuora {

  trait Service {
    def getSubscription(subscriptionName: String): Task[String]

    def extend6For6RatePlan(model: ExtendIntroRatePlanModel): Task[Unit]

    def postpone6For6RatePlan(model: PushBackIntroRatePlanModel): Task[Unit]

    def postponeMainRatePlan(model: PushBackMainRatePlanModel): Task[Unit]
  }

  def getSubscription(subscriptionName: String): RIO[Zuora, String] =
    RIO.accessM(_.get.getSubscription(subscriptionName))

  def extend6For6RatePlan(model: ExtendIntroRatePlanModel): RIO[Zuora, Unit] =
    RIO.accessM(_.get.extend6For6RatePlan(model))

  def postpone6For6RatePlan(model: PushBackIntroRatePlanModel): RIO[Zuora, Unit] =
    RIO.accessM(_.get.postpone6For6RatePlan(model))

  def postponeMainRatePlan(model: PushBackMainRatePlanModel): RIO[Zuora, Unit] =
    RIO.accessM(_.get.postponeMainRatePlan(model))
}
