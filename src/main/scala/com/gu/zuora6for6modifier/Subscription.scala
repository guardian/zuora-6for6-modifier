package com.gu.zuora6for6modifier

import io.circe.generic.auto._
import io.circe.parser.decode

case class Subscription(ratePlans: List[Plan])

case class Plan(
  id: String,
  productRatePlanId: String,
  ratePlanName: String,
  lastChangeType: Option[String],
  ratePlanCharges: List[Charge]
)

case class Charge(
  id: String,
  productRatePlanChargeId: String,
  effectiveStartDate: String,
  effectiveEndDate: String
)

object Subscription {

  val keyDate = "2019-12-27"
  val productRatePlanNamePrefix6For6 = "GW Oct 18 - Six for Six"
  val productRatePlanNamePrefixMain = "GW Oct 18 - Quarterly"

  def extractData(subName: String, json: String): Either[String, SubData] = {
    for {
      sub <- decode[Subscription](json).left.map(_.getMessage)
      valid <- validForModification(sub)
      plan6For6 <- valid.ratePlans
        .find(_.ratePlanName.startsWith(productRatePlanNamePrefix6For6))
        .toRight("Can't find 6 for 6 plan")
      charge6For6 <- plan6For6.ratePlanCharges.headOption.toRight("Can't find 6 for 6 charge")
      planMain <- valid.ratePlans
        .find(_.ratePlanName.startsWith(productRatePlanNamePrefixMain))
        .toRight("Can't find main plan")
      chargeMain <- planMain.ratePlanCharges.headOption.toRight("Can't find main charge")
      productPlanId6For6 <- Config.Zuora.productPlanId6For6
        .get(plan6For6.ratePlanName)
        .toRight("Can't find corresponding 6 for 7 plan")
    } yield SubData(
      subName,
      productPlanId6For6,
      productPlanIdMain = Config.Zuora.productPlanIdMain,
      start6For6Date = charge6For6.effectiveStartDate,
      startMainDate = chargeMain.effectiveStartDate,
      planId6For6 = plan6For6.id,
      planIdMain = planMain.id
    )
  }

  private def validForModification(subscription: Subscription): Either[String, Subscription] = {
    def test(p: Subscription => Boolean)(msg: String) =
      if (p(subscription)) Right(subscription) else Left(msg)

    for {
      _ <- test(_.ratePlans.length == 2)("Wrong number of plans")
      _ <- test(
        _.ratePlans
          .find(_.ratePlanName.startsWith(productRatePlanNamePrefix6For6))
          .exists(_.lastChangeType.isEmpty)
      )("No original 6 for 6 plan")
      _ <- test(
        _.ratePlans
          .find(_.ratePlanName.startsWith(productRatePlanNamePrefixMain))
          .exists(_.lastChangeType.isEmpty)
      )("No original main plan")
      _ <- test(
        _.ratePlans
          .find(_.ratePlanName.startsWith(productRatePlanNamePrefix6For6))
          .exists(_.ratePlanCharges.headOption.exists { charge =>
            charge.effectiveStartDate < keyDate &&
            charge.effectiveEndDate >= keyDate
          })
      )("Doesn't include key date")
    } yield subscription
  }
}
