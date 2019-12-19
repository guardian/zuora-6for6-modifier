package com.gu.zuora6for6modifier

import io.circe.generic.auto._
import io.circe.parser.decode
import scalaj.http.{BaseHttp, Http, HttpOptions}
import zio.{Has, Task, ZIO, ZLayer}

object ZuoraLive {

  object HttpWithLongTimeout
      extends BaseHttp(
        options = Seq(
          HttpOptions.connTimeout(5000),
          HttpOptions.readTimeout(30000),
          HttpOptions.followRedirects(false)
        )
      )

  val host: String = Config.Zuora.stage match {
    case "DEV" | "UAT" => "https://rest.apisandbox.zuora.com"
    case "PROD"        => "https://rest.zuora.com"
  }

  val impl: ZLayer[Any, Throwable, Zuora] = {

    val getAccessToken: Task[String] = {
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
        case _ => ZIO.fail(new RuntimeException(s"Failed to authenticate with Zuora: ${response.toString}"))
      }
    }

    val zuoraLayer: ZLayer[Has[String], Nothing, Zuora] = {
      ZLayer.fromService { accessToken =>
        new Zuora.Service {

          def putSubscription(data: SubscriptionData, body: SubscriptionData => String): Task[Unit] = {
            case class PutResponse(success: Boolean)
            val response =
              HttpWithLongTimeout(s"$host/v1/subscriptions/${data.subscriptionName}")
                .header("Authorization", s"Bearer $accessToken")
                .header("Content-type", "application/json")
                .put(body(data))
                .method("PUT")
                .asString
            for {
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

          def getSubscription(subName: String): Task[String] = {
            val response = HttpWithLongTimeout(s"$host/v1/subscriptions/$subName")
              .header("Authorization", s"Bearer $accessToken")
              .method("GET")
              .asString
            for {
              responseBody <- response.code match {
                case 200 => ZIO.succeed(response.body)
                case _   => ZIO.fail(new RuntimeException(s"Failed to look up $subName: ${response.toString}"))
              }
            } yield responseBody
          }

          def extend6For6RatePlan(subscriptionData: SubscriptionData): Task[Unit] =
            putSubscription(subscriptionData, PutRequests.extendTo7Weeks)

          def postpone6For6RatePlan(subscriptionData: SubscriptionData): Task[Unit] =
            putSubscription(subscriptionData, PutRequests.start6For6RatePlanWeekLater)

          def postponeMainRatePlan(subscriptionData: SubscriptionData): Task[Unit] =
            putSubscription(subscriptionData, PutRequests.startMainRatePlanWeekLater)
        }
      }
    }

    getAccessToken.toLayer >>> zuoraLayer
  }

  object PutRequests {

    def extendTo7Weeks(data: SubscriptionData): String =
      s"""
         |{
         |  "add": [
         |    {
         |      "productRatePlanId": "${data.productPlanId6For6}",
         |      "contractEffectiveDate": "${data.start6For6Date}",
         |      "chargeOverrides": [
         |        {
         |          "productRatePlanChargeId": "${data.productChargeId6For6}",
         |          "billingPeriod": "Specific_Weeks",
         |          "specificBillingPeriod": 7
         |        }
         |      ]
         |    },
         |    {
         |      "productRatePlanId": "${data.productPlanIdMain}"
         |      "contractEffectiveDate": "${data.startMainDate}",
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${data.planId6For6}",
         |      "contractEffectiveDate": "${data.start6For6Date}"
         |    },
         |    {
         |      "ratePlanId": "${data.planIdMain}",
         |      "contractEffectiveDate": "${data.start6For6Date}"
         |    }
         |  ]
         |}
         |""".stripMargin

    def start6For6RatePlanWeekLater(data: SubscriptionData): String =
      s"""
         |{
         |  "add": [
         |    {
         |      "productRatePlanId": "${data.productPlanId6For6}",
         |      "contractEffectiveDate": "${Config.keyDatePlusWeek}"
         |    },
         |    {
         |      "productRatePlanId": "${data.productPlanIdMain}",
         |      "contractEffectiveDate": "${Config.keyDatePlus7Weeks}"
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${data.planId6For6}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    },
         |    {
         |      "ratePlanId": "${data.planIdMain}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    }
         |  ]
         |}
         |""".stripMargin

    def startMainRatePlanWeekLater(data: SubscriptionData): String =
      s"""
         |{
         |  "add": [
         |    {
         |      "productRatePlanId": "${data.productPlanIdMain}",
         |      "contractEffectiveDate": "${Config.keyDatePlusWeek}"
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${data.planIdMain}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    }
         |  ]
         |}
         |""".stripMargin
  }
}
