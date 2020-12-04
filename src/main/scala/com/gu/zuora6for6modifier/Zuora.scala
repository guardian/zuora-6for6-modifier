package com.gu.zuora6for6modifier

import zio.{RIO, Task}

object Zuora {

  trait Service {
    def getSubscription(subName: String): Task[String]

    def extendSubscription(subData: SubscriptionData): Task[Unit]

    def postponeSubscription(subData: SubscriptionData): Task[Unit]
  }

  def getSubscription(subName: String): RIO[Zuora, String] =
    RIO.accessM(_.get.getSubscription(subName))

  def extendSubscription(subData: SubscriptionData): RIO[Zuora, Unit] =
    RIO.accessM(_.get.extendSubscription(subData))

  def postponeSubscription(subData: SubscriptionData): RIO[Zuora, Unit] =
    RIO.accessM(_.get.postponeSubscription(subData))
}
