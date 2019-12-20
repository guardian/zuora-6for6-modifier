package com.gu.zuora6for6modifier

import io.circe.generic.auto._
import io.circe.parser.decode
import scalaj.http.{BaseHttp, Http, HttpOptions}
import zio.{RIO, Task, ZIO}

trait Zuora {
  val zuora: Zuora.Service
}

object Zuora {

  trait Service {
    def getSubscription(subName: String): Task[String]
    def extendSubscription(subData: SubscriptionData): Task[Unit]
    def postponeSubscription(subData: SubscriptionData): Task[Unit]
  }

  object > {

    def getSubscription(subName: String): RIO[Zuora, String] =
      ZIO.accessM(_.zuora.getSubscription(subName))

    def extendSubscription(subData: SubscriptionData): RIO[Zuora, Unit] =
      ZIO.accessM(_.zuora.extendSubscription(subData))

    def postponeSubscription(subData: SubscriptionData): RIO[Zuora, Unit] =
      ZIO.accessM(_.zuora.postponeSubscription(subData))
  }
}

trait ZuoraLive extends Zuora {

  val zuora: Zuora.Service = new Zuora.Service {

    private val host: String =
      Config.Zuora.stage match {
        case "DEV" | "UAT" => "https://rest.apisandbox.zuora.com"
        case "PROD"        => "https://rest.zuora.com"
      }

    private object HttpWithLongTimeout
        extends BaseHttp(
          options = Seq(
            HttpOptions.connTimeout(5000),
            HttpOptions.readTimeout(30000),
            HttpOptions.followRedirects(false)
          )
        )

    private lazy val token: Task[String] = {
      case class AccessToken(access_token: String)
      val url = s"$host/oauth/token"
      val response = Http(url)
        .postForm(
          Seq(
            "client_id" -> Config.Zuora.clientId,
            "client_secret" -> Config.Zuora.clientSecret,
            "grant_type" -> "client_credentials"
          )
        )
        .asString
      response.code match {
        case 200 =>
          decode[AccessToken](response.body) match {
            case Left(e)      => ZIO.fail(e)
            case Right(token) => ZIO.succeed(token.access_token)
          }
        case _ => ZIO.fail(new RuntimeException(s"Failed to authenticate with Zuora: $response"))
      }
    }

    def getSubscription(subName: String): Task[String] =
      for {
        accessToken <- token
        response = HttpWithLongTimeout(s"$host/v1/subscriptions/$subName")
          .header("Authorization", s"Bearer $accessToken")
          .method("GET")
          .asString
        responseBody <- response.code match {
          case 200 => ZIO.succeed(response.body)
          case _   => ZIO.fail(new RuntimeException(s"Failed to look up $subName: $response"))
        }
      } yield responseBody

    private def putSubscription(
        subData: SubscriptionData,
        body: SubscriptionData => String
    ): Task[Unit] = {
      case class PutResponse(success: Boolean)
      for {
        accessToken <- token
        response = HttpWithLongTimeout(s"$host/v1/subscriptions/${subData.subscriptionName}")
          .header("Authorization", s"Bearer $accessToken")
          .header("Content-type", "application/json")
          .put(body(subData))
          .method("PUT")
          .asString
        _ <- response.code match {
          case 200 =>
            decode[PutResponse](response.body).map(_.success) match {
              case Left(e)      => ZIO.fail(e)
              case Right(false) => ZIO.fail(new RuntimeException(response.body))
              case Right(true)  => ZIO.succeed(())
            }
          case _ =>
            ZIO.fail(new RuntimeException(response.body))
        }
      } yield ()
    }

    def extendSubscription(subscriptionData: SubscriptionData): Task[Unit] =
      putSubscription(subscriptionData, PutRequests.extendTo2Months)

    def postponeSubscription(subscriptionData: SubscriptionData): Task[Unit] =
      putSubscription(subscriptionData, PutRequests.startWeekLater)
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
         |      "productRatePlanId": "${subData.productPlanId6For6}",
         |      "contractEffectiveDate": "${Config.keyDatePlusWeek}"
         |    },
         |    {
         |      "productRatePlanId": "${subData.productPlanIdMain}",
         |      "contractEffectiveDate": "${Config.keyDatePlus7Weeks}"
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
}

object ZuoraLive extends ZuoraLive
