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
  val productRatePlanNamePrefixMain = "GW Oct 18 - "

  private def isIntroPlan(plan: Plan) =
    plan.ratePlanName.startsWith(productRatePlanNamePrefix6For6)

  private def isMainPlan(plan: Plan) =
    plan.ratePlanName.startsWith(productRatePlanNamePrefixMain) && !isIntroPlan(plan)

  private def test(sub: Subscription, p: Subscription => Boolean)(msg: String) =
    if (p(sub)) Right(sub) else Left(new RuntimeException(msg))

  private def hasChargeStartingOnDate(date: String)(plan: Plan) =
    plan.ratePlanCharges.headOption.exists { charge => charge.effectiveStartDate == date }

  def extractDataForExtending(subscriptionName: String, json: String): Either[Throwable, ExtendIntroRatePlanModel] = {

    def plus7Weeks(s: String): String = LocalDate.parse(s).plusWeeks(7).toString

    for {
      sub <- decode[Subscription](json)
      valid <- validForExtending(sub)
      plan6For6 <- valid.ratePlans
        .find(plan => isIntroPlan(plan))
        .toRight(new RuntimeException("Can't find 6 for 6 plan"))
      charge6For6 <- plan6For6.ratePlanCharges.headOption.toRight(new RuntimeException("Can't find 6 for 6 charge"))
      planMain <- valid.ratePlans.find(isMainPlan).toRight(new RuntimeException("Can't find main plan"))
    } yield ExtendIntroRatePlanModel(
      subscriptionName,
      productPlanId6For6 = plan6For6.productRatePlanId,
      productChargeId6For6 = charge6For6.productRatePlanChargeId,
      productPlanIdMain = planMain.productRatePlanId,
      start6For6Date = charge6For6.effectiveStartDate,
      startMainDate = plus7Weeks(charge6For6.effectiveStartDate),
      planId6For6 = plan6For6.id,
      planIdMain = planMain.id
    )
  }

  private def validForExtending(subscription: Subscription): Either[Throwable, Subscription] =
    for {
      _ <- test(subscription, _.ratePlans.count(isIntroPlan) == 1)("Doesn't have precisely one 6 for 6 plan")
      _ <- test(subscription, _.ratePlans.count(isMainPlan) == 1)("Has multiple main plans")
      _ <- test(subscription, _.ratePlans.find(isIntroPlan).exists(_.lastChangeType.isEmpty))(
        "No original 6 for 6 plan"
      )
      _ <- test(subscription, _.ratePlans.find(isMainPlan).exists(_.lastChangeType.isEmpty))("No original main plan")
      _ <- test(
        subscription,
        _.ratePlans
          .find(isIntroPlan)
          .exists(_.ratePlanCharges.headOption.exists { charge =>
            charge.effectiveStartDate < Config.keyDate && charge.effectiveEndDate > Config.keyDate
          })
      )("Doesn't include key date")
    } yield subscription

  def extractDataForPostponing(
      subscriptionName: String,
      json: String
  ): Either[Throwable, PushBackIntroRatePlanModel] = {
    for {
      sub <- decode[Subscription](json)
      valid <- validForPostponing(sub)
      plan6For6 <- valid.ratePlans.find(isIntroPlan).toRight(new RuntimeException("Can't find 6 for 6 plan"))
      planMain <- valid.ratePlans.find(isMainPlan).toRight(new RuntimeException("Can't find main plan"))
    } yield PushBackIntroRatePlanModel(
      subscriptionName,
      productPlanId6For6 = plan6For6.productRatePlanId,
      productPlanIdMain = planMain.productRatePlanId,
      planId6For6 = plan6For6.id,
      planIdMain = planMain.id
    )
  }

  private def validForPostponing(subscription: Subscription): Either[Throwable, Subscription] =
    for {
      _ <- test(subscription, _.ratePlans.length == 2)("Wrong number of plans")
      _ <- test(subscription, _.ratePlans.find(isIntroPlan).exists(_.lastChangeType.isEmpty))(
        "No original 6 for 6 plan"
      )
      _ <- test(subscription, _.ratePlans.find(isMainPlan).exists(_.lastChangeType.isEmpty))("No original main plan")
      _ <- test(subscription, _.ratePlans.find(isIntroPlan).exists(hasChargeStartingOnDate(Config.keyDate)))(
        "Doesn't include key date"
      )
    } yield subscription

  def extractDataForPostponingMainRatePlan(
      subscriptionName: String,
      json: String
  ): Either[Throwable, PushBackMainRatePlanModel] = {
    for {
      sub <- decode[Subscription](json)
      valid <- validForPostponingMainRatePlan(sub)
      planMain <- valid.ratePlans.find(isMainPlan).toRight(new RuntimeException("Can't find main plan"))
    } yield PushBackMainRatePlanModel(
      subscriptionName,
      productPlanIdMain = planMain.productRatePlanId,
      planIdMain = planMain.id
    )
  }

  private def validForPostponingMainRatePlan(subscription: Subscription): Either[Throwable, Subscription] =
    for {
      _ <- test(subscription, _.ratePlans.count(isMainPlan) == 1)("Has multiple main plans")
      _ <- test(subscription, _.ratePlans.find(isMainPlan).exists(_.lastChangeType.isEmpty))("No original main plan")
      _ <- test(subscription, _.ratePlans.find(isMainPlan).exists(hasChargeStartingOnDate(Config.keyDate)))(
        "Doesn't include key date"
      )
    } yield subscription
}
