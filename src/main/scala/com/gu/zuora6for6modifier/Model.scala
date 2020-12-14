package com.gu.zuora6for6modifier

sealed trait Model {
  def subscriptionName: String
}

case class ExtendIntroRatePlanModel(
    subscriptionName: String,
    productPlanId6For6: String,
    productChargeId6For6: String,
    productPlanIdMain: String,
    start6For6Date: String,
    startMainDate: String,
    planId6For6: String,
    planIdMain: String
) extends Model

case class PushBackIntroRatePlanModel(
    subscriptionName: String,
    productPlanId6For6: String,
    productPlanIdMain: String,
    planId6For6: String,
    planIdMain: String
) extends Model

case class PushBackMainRatePlanModel(
    subscriptionName: String,
    productPlanIdMain: String,
    planIdMain: String
) extends Model
