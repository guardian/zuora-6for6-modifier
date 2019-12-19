package com.gu.zuora6for6modifier

import zio.{RIO, Task}

object Zuora {

  trait Service {
    def getSubscription(subscriptionName: String): Task[String]

    def extend6For6RatePlan(subscriptionData: SubscriptionData): Task[Unit]

    def postpone6For6RatePlan(subscriptionData: SubscriptionData): Task[Unit]

    def postponeMainRatePlan(subscriptionData: SubscriptionData): Task[Unit]
  }

  def getSubscription(subscriptionName: String): RIO[Zuora, String] =
    RIO.accessM(_.get.getSubscription(subscriptionName))

  def extend6For6RatePlan(subscriptionData: SubscriptionData): RIO[Zuora, Unit] =
    RIO.accessM(_.get.extend6For6RatePlan(subscriptionData))

  def postpone6For6RatePlan(subscriptionData: SubscriptionData): RIO[Zuora, Unit] =
    RIO.accessM(_.get.postpone6For6RatePlan(subscriptionData))

  def postponeMainRatePlan(subscriptionData: SubscriptionData): RIO[Zuora, Unit] =
    RIO.accessM(_.get.postponeMainRatePlan(subscriptionData))
}
