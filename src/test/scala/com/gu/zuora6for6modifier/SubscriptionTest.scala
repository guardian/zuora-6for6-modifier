package com.gu.zuora6for6modifier

import io.circe.parser.parse
import munit.FunSuite

import scala.io.Source

class SubscriptionTest extends FunSuite {

  private def toJsonString(resource: String): String = {
    val json = Source.fromResource(resource).mkString
    parse(json).map(_.spaces2).getOrElse(throw new RuntimeException(s"Can't decode:\n$json"))
  }

  test("extractDataForExtending extracts correct data from valid Json") {
    val data = Subscription.extractDataForExtending(
      subscriptionName = "subNum",
      json = toJsonString("ToExtend.json")
    )
    assertEquals(
      data,
      Right(
        SubscriptionData(
          subscriptionName = "subNum",
          productPlanId6For6 = "2c92c0f965f212210165f69b94c92d66",
          productChargeId6For6 = "2c92c0f865f204440165f69f407d66f1",
          productPlanIdMain = "2c92c0f965dc30640165f150c0956859",
          start6For6Date = "2020-12-18",
          startMainDate = "2021-02-05",
          planId6For6 = "id6for6",
          planIdMain = "idMain"
        )
      )
    )
  }

  test("extractDataForExtending won't try to extend an already extended subscription") {
    val data = Subscription.extractDataForExtending(
      subscriptionName = "subNum",
      json = toJsonString("AlreadyExtended.json")
    )
    assert(data.isLeft)
  }
}
