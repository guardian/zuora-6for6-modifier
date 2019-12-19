package com.gu.zuora6for6modifier

import java.time.LocalDate

import com.typesafe.config.ConfigFactory

object Config {
  private lazy val conf = ConfigFactory.load

  val keyDate = "2019-12-27"
  val keyDatePlusWeek = LocalDate.parse(keyDate).plusWeeks(1).toString
  val keyDatePlus7Weeks = LocalDate.parse(keyDate).plusWeeks(7).toString

  object Zuora {
    lazy val stage: String = conf.getString("zuora.stage")
    lazy val clientId: String = conf.getString("zuora.client_id")
    lazy val clientSecret: String = conf.getString("zuora.client_secret")
  }
}
