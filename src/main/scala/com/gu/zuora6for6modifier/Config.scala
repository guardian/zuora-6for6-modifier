package com.gu.zuora6for6modifier

import com.typesafe.config.ConfigFactory

object Config {
  private val conf = ConfigFactory.load

  object Zuora {
    lazy val stage: String = conf.getString("zuora.stage")
    lazy val client_id: String = conf.getString("zuora.client_id")
    lazy val client_secret: String = conf.getString("zuora.client_secret")
    lazy val productPlanId6For6: Map[String, String] = Map(
      "GW Oct 18 - Six for Six - Domestic" -> conf.getString("zuora.productPlanId6For6.Domestic"),
      "GW Oct 18 - Six for Six - ROW" -> conf.getString("zuora.productPlanId6For6.ROW")
    )
    lazy val productPlanIdMain: String = conf.getString("zuora.productPlanIdMain")
  }
}
