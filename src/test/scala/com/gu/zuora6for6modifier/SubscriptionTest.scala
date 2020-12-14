package com.gu.zuora6for6modifier

import munit.FunSuite

import scala.io.Source

class SubscriptionTest extends FunSuite {

  private def contentOf(resourceName: String): String =
    Source.fromResource(resourceName).mkString

  private def test(methodName: String)(testDescription: String)(body: => Any): Unit =
    super.test(s"$methodName: $testDescription")(body)

  private val testExtractDataForExtending = test("extractDataForExtending") _
  private val testExtractDataForPostponing = test("extractDataForPostponing") _
  private val testExtractDataForPostponingMainRatePlan = test("extractDataForPostponingMainRatePlan") _

  testExtractDataForExtending("extracts correct data from valid subscription") {
    val data = Subscription.extractDataForExtending(
      subscriptionName = "subNum",
      json = contentOf("ToExtend.json")
    )
    assertEquals(
      data,
      Right(
        ExtendIntroRatePlanModel(
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

  testExtractDataForExtending("won't try to extend an already extended rate plan") {
    val data = Subscription.extractDataForExtending(
      subscriptionName = "subNum",
      json = contentOf("AlreadyExtended.json")
    )
    assert(data.isLeft)
  }

  testExtractDataForPostponing("extracts correct data from valid subscription") {
    val data = Subscription.extractDataForPostponing(
      subscriptionName = "subNum",
      json = contentOf("FirstIssueInChristmasWeek.json")
    )
    assertEquals(
      data,
      Right(
        PushBackIntroRatePlanModel(
          subscriptionName = "subNum",
          productPlanId6For6 = "2c92a0086619bf8901661aaac94257fe",
          productPlanIdMain = "2c92a0fe6619b4b301661aa494392ee2",
          planId6For6 = "id6for6",
          planIdMain = "idMain"
        )
      )
    )
  }

  testExtractDataForPostponing("won't try to push back an already postponed 6-for-6 rate plan") {
    val data = Subscription.extractDataForPostponing(
      subscriptionName = "subNum",
      json = contentOf("AlreadyPushedBack.json")
    )
    assert(data.isLeft)
  }

  testExtractDataForPostponingMainRatePlan("extracts correct data from valid subscription") {
    val data = Subscription.extractDataForPostponingMainRatePlan(
      subscriptionName = "subNum",
      json = contentOf("FirstBillAfter6For6InChristmasWeek.json")
    )
    assertEquals(
      data,
      Right(
        PushBackMainRatePlanModel(
          subscriptionName = "subNum",
          productPlanIdMain = "2c92a0fe6619b4b301661aa494392ee2",
          planIdMain = "idMain"
        )
      )
    )
  }

  testExtractDataForPostponingMainRatePlan("won't try to push back an already postponed main rate plan") {
    val data = Subscription.extractDataForPostponingMainRatePlan(
      subscriptionName = "subNum",
      json = contentOf("MainRatePlanAlreadyPushedBack.json")
    )
    assert(data.isLeft)
  }
}
