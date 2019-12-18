package com.gu.zuora6for6modifier

import java.time.LocalDate

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

  val productRatePlanNamePrefix6For6 = "GW Oct 18 - Six for Six"
  val productRatePlanNamePrefixMain = "GW Oct 18 - Quarterly"

  def extractDataForExtending(
      subscriptionName: String,
      json: String
  ): Either[String, SubscriptionData] = {
    def plus2Months(s: String): String = LocalDate.parse(s).plusMonths(2).toString
    for {
      sub <- decode[Subscription](json).left.map(_.getMessage)
      valid <- validForExtending(sub)
      plan6For6 <- valid.ratePlans
        .find(_.ratePlanName.startsWith(productRatePlanNamePrefix6For6))
        .toRight("Can't find 6 for 6 plan")
      charge6For6 <- plan6For6.ratePlanCharges.headOption.toRight("Can't find 6 for 6 charge")
      planMain <- valid.ratePlans
        .find(_.ratePlanName.startsWith(productRatePlanNamePrefixMain))
        .toRight("Can't find main plan")
    } yield SubscriptionData(
      subscriptionName,
      productPlanId6For6 = plan6For6.productRatePlanId,
      productChargeId6For6 = charge6For6.productRatePlanChargeId,
      productPlanIdMain = planMain.productRatePlanId,
      start6For6Date = charge6For6.effectiveStartDate,
      startMainDate = plus2Months(charge6For6.effectiveStartDate),
      planId6For6 = plan6For6.id,
      planIdMain = planMain.id
    )
  }

  private def validForExtending(subscription: Subscription): Either[String, Subscription] = {
    def test(p: Subscription => Boolean)(msg: String) =
      if (p(subscription)) Right(subscription) else Left(msg)

    for {
      _ <- test(
        _.ratePlans.count(_.ratePlanName.startsWith(productRatePlanNamePrefix6For6)) == 1
      )("Has multiple 6 for 6 plans")
      _ <- test(
        _.ratePlans.count(_.ratePlanName.startsWith(productRatePlanNamePrefixMain)) == 1
      )("Has multiple main plans")
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
            charge.effectiveStartDate < Config.keyDate && charge.effectiveEndDate >= Config.keyDate
          })
      )("Doesn't include key date")
    } yield subscription
  }

  def extractDataForPostponing(subName: String, json: String): Either[String, SubscriptionData] = {
    def plusWeek(s: String): String = LocalDate.parse(s).plusWeeks(1).toString
    for {
      sub <- decode[Subscription](json).left.map(_.getMessage)
      valid <- validForPostponing(sub)
      plan6For6 <- valid.ratePlans
        .find(_.ratePlanName.startsWith(productRatePlanNamePrefix6For6))
        .toRight("Can't find 6 for 6 plan")
      charge6For6 <- plan6For6.ratePlanCharges.headOption.toRight("Can't find 6 for 6 charge")
      planMain <- valid.ratePlans
        .find(_.ratePlanName.startsWith(productRatePlanNamePrefixMain))
        .toRight("Can't find main plan")
      chargeMain <- planMain.ratePlanCharges.headOption.toRight("Can't find 6 for 6 charge")
    } yield SubscriptionData(
      subName,
      productPlanId6For6 = plan6For6.productRatePlanId,
      productChargeId6For6 = charge6For6.productRatePlanChargeId,
      productPlanIdMain = planMain.productRatePlanId,
      start6For6Date = plusWeek(charge6For6.effectiveStartDate),
      startMainDate = plusWeek(chargeMain.effectiveStartDate),
      planId6For6 = plan6For6.id,
      planIdMain = planMain.id
    )
  }

  private def validForPostponing(subscription: Subscription): Either[String, Subscription] = {
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
            charge.effectiveStartDate == Config.keyDate
          })
      )("Doesn't include key date")
    } yield subscription
  }
}
