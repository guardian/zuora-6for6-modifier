package com.gu.zuora6for6modifier

import com.gu.zuora6for6modifier.ZuoraHostSelector.host
import io.circe.generic.auto._
import io.circe.parser.decode
import scalaj.http.{BaseHttp, Http, HttpOptions}
import zio.ZIO

trait Zuora {
  val zuora: Zuora.Service[Any]
}

object Zuora {

  trait Service[R] {
    val token: ZIO[R, Throwable, String]
    def getSubscription(accessToken: String, subName: String): ZIO[R, Throwable, String]
    def putSubscription(
        accessToken: String,
        subData: SubscriptionData,
        body: SubscriptionData => String
    ): ZIO[R, Throwable, String]
  }

  object > extends Zuora.Service[Zuora] {

    val token: ZIO[Zuora, Throwable, String] = ZIO.accessM(_.zuora.token)

    def getSubscription(accessToken: String, subName: String): ZIO[Zuora, Throwable, String] =
      ZIO.accessM(_.zuora.getSubscription(accessToken, subName))

    def putSubscription(
        accessToken: String,
        subData: SubscriptionData,
        body: SubscriptionData => String
    ): ZIO[Zuora, Throwable, String] =
      ZIO.accessM(_.zuora.putSubscription(accessToken, subData, body))
  }
}

trait ZuoraLive extends Zuora {

  val zuora: Zuora.Service[Any] = new Zuora.Service[Any] {

    val token: ZIO[Any, Throwable, String] = {
      val response = Http(s"$host/oauth/token")
        .postForm(
          Seq(
            "client_id" -> Config.Zuora.client_id,
            "client_secret" -> Config.Zuora.client_secret,
            "grant_type" -> "client_credentials"
          )
        )
        .asString
      response.code match {
        case 200 =>
          decode[AccessToken](response.body) match {
            case Left(e)  => ZIO.fail(e)
            case Right(t) => ZIO.succeed(t.access_token)
          }
        case _ => ZIO.fail(new RuntimeException(s"Failed to authenticate with Zuora: $response"))
      }
    }

    def getSubscription(
        accessToken: String,
        subName: String
    ): ZIO[Any, Throwable, String] = {
      val response = HttpWithLongTimeout(s"$host/v1/subscriptions/$subName")
        .header("Authorization", s"Bearer $accessToken")
        .method("GET")
        .asString
      response.code match {
        case 200 => ZIO.succeed(response.body)
        case _   => ZIO.fail(new RuntimeException(s"Failed to look up $subName: $response"))
      }
    }

    def putSubscription(
        accessToken: String,
        subData: SubscriptionData,
        body: SubscriptionData => String
    ): ZIO[Any, Throwable, String] = {
      val response =
        HttpWithLongTimeout(s"$host/v1/subscriptions/${subData.subscriptionName}")
          .header("Authorization", s"Bearer $accessToken")
          .header("Content-type", "application/json")
          .put(body(subData))
          .method("PUT")
          .asString
      response.code match {
        case 200 =>
          decode[PutResponse](response.body).map(_.success) match {
            case Left(e)      => ZIO.fail(e)
            case Right(false) => ZIO.fail(new RuntimeException(response.body))
            case Right(true)  => ZIO.succeed(response.body)
          }
        case _ =>
          throw new RuntimeException(response.body)
      }
    }
  }
}

object PutRequests {

  def extendTo2Months(subData: SubscriptionData): String =
    s"""
       |{
       |  "add": [
       |    {
       |      "contractEffectiveDate": "${subData.start6For6Date}",
       |      "productRatePlanId": "${subData.productPlanId6For6}",
       |      "chargeOverrides": [
       |        {
       |          "productRatePlanChargeId": "${subData.productChargeId6For6}",
       |          "billingPeriod": "Specific_Months",
       |          "specificBillingPeriod": 2
       |        }
       |      ]
       |    },
       |    {
       |      "contractEffectiveDate": "${subData.startMainDate}",
       |      "productRatePlanId": "${subData.productPlanIdMain}"
       |    }
       |  ],
       |  "remove": [
       |    {
       |      "ratePlanId": "${subData.planId6For6}",
       |      "contractEffectiveDate": "${subData.start6For6Date}"
       |    },
       |    {
       |      "ratePlanId": "${subData.planIdMain}",
       |      "contractEffectiveDate": "${subData.start6For6Date}"
       |    }
       |  ]
       |}
       |""".stripMargin

  def startWeekLater(subData: SubscriptionData): String =
    s"""
         |{
         |  "add": [
         |    {
         |      "contractEffectiveDate": "${Config.keyDatePlusWeek}",
         |      "productRatePlanId": "${subData.productPlanId6For6}"
         |    },
         |    {
         |      "contractEffectiveDate": "${Config.keyDatePlus7Weeks}",
         |      "productRatePlanId": "${subData.productPlanIdMain}"
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${subData.planId6For6}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    },
         |    {
         |      "ratePlanId": "${subData.planIdMain}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    }
         |  ]
         |}
         |""".stripMargin
}

object ZuoraHostSelector {
  val host: String =
    Config.Zuora.stage match {
      case "DEV" | "UAT" => "https://rest.apisandbox.zuora.com"
      case "PROD"        => "https://rest.zuora.com"
    }
}

object HttpWithLongTimeout
    extends BaseHttp(
      options = Seq(
        HttpOptions.connTimeout(5000),
        HttpOptions.readTimeout(30000),
        HttpOptions.followRedirects(false)
      )
    )

object ZuoraLive extends ZuoraLive
