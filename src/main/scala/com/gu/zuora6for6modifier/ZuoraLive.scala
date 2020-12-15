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

          def putSubscription[A <: Model](model: A, body: A => String): Task[Unit] = {
            case class PutResponse(success: Boolean)
            val response =
              HttpWithLongTimeout(s"$host/v1/subscriptions/${model.subscriptionName}")
                .header("Authorization", s"Bearer $accessToken")
                .header("Content-type", "application/json")
                .put(body(model))
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

          def extend6For6RatePlan(model: ExtendIntroRatePlanModel): Task[Unit] =
            putSubscription(model, PutRequests.extendTo7Weeks)

          def postpone6For6RatePlan(model: PushBackIntroRatePlanModel): Task[Unit] =
            putSubscription(model, PutRequests.start6For6RatePlanWeekLater)

          def postponeMainRatePlan(model: PushBackMainRatePlanModel): Task[Unit] =
            putSubscription(model, PutRequests.startMainRatePlanWeekLater)
        }
      }
    }

    getAccessToken.toLayer >>> zuoraLayer
  }

  object PutRequests {

    def extendTo7Weeks(model: ExtendIntroRatePlanModel): String =
      s"""
         |{
         |  "add": [
         |    {
         |      "productRatePlanId": "${model.productPlanId6For6}",
         |      "contractEffectiveDate": "${model.start6For6Date}",
         |      "chargeOverrides": [
         |        {
         |          "productRatePlanChargeId": "${model.productChargeId6For6}",
         |          "billingPeriod": "Specific_Weeks",
         |          "specificBillingPeriod": 7
         |        }
         |      ]
         |    },
         |    {
         |      "productRatePlanId": "${model.productPlanIdMain}",
         |      "contractEffectiveDate": "${model.startMainDate}"
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${model.planId6For6}",
         |      "contractEffectiveDate": "${model.start6For6Date}"
         |    },
         |    {
         |      "ratePlanId": "${model.planIdMain}",
         |      "contractEffectiveDate": "${model.start6For6Date}"
         |    }
         |  ]
         |}
         |""".stripMargin

    def start6For6RatePlanWeekLater(model: PushBackIntroRatePlanModel): String =
      s"""
         |{
         |  "add": [
         |    {
         |      "productRatePlanId": "${model.productPlanId6For6}",
         |      "contractEffectiveDate": "${Config.keyDatePlusWeek}"
         |    },
         |    {
         |      "productRatePlanId": "${model.productPlanIdMain}",
         |      "contractEffectiveDate": "${Config.keyDatePlus7Weeks}"
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${model.planId6For6}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    },
         |    {
         |      "ratePlanId": "${model.planIdMain}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    }
         |  ]
         |}
         |""".stripMargin

    def startMainRatePlanWeekLater(model: PushBackMainRatePlanModel): String =
      s"""
         |{
         |  "add": [
         |    {
         |      "productRatePlanId": "${model.productPlanIdMain}",
         |      "contractEffectiveDate": "${Config.keyDatePlusWeek}"
         |    }
         |  ],
         |  "remove": [
         |    {
         |      "ratePlanId": "${model.planIdMain}",
         |      "contractEffectiveDate": "${Config.keyDate}"
         |    }
         |  ]
         |}
         |""".stripMargin
  }
}
