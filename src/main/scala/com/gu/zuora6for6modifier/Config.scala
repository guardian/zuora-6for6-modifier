package com.gu.zuora6for6modifier

import com.typesafe.config.ConfigFactory

object Config {
  private val conf = ConfigFactory.load

  object Zuora {
    lazy val stage: String = conf.getString("zuora.stage")
    lazy val client_id: String = conf.getString("zuora.client_id")
    lazy val client_secret: String = conf.getString("zuora.client_secret")
  }
}
