package com.riskified.repro

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Authority, Host}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.newrelic.api.agent.{NewRelic, Token, Trace}
import scala.concurrent.{ExecutionContext, Future}

object SampleApp extends HttpApp with App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val local = Authority(Host("localhost"), 8822)
  val checkPath = "check"
  val checkUri = Uri(s"http://$local/$checkPath")

  @Trace(async = true)
  def instrumented(
      request: HttpRequest,
      token: Token = NewRelic.getAgent.getTransaction.getToken
  ): Future[HttpResponse] = {
    token.link()
    Http().singleRequest(request)
  }

  def uninstrumented(request: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(request)

  val route1 = path("") {
    val handling = for {
      txId1 <- Future { TxId.current.tap(txId => println(s"entered route1: $txId")) }
      txId2 = TxId.current.tap(txId => println(s"still in route1: $txId"))
      txId3 <- Future { TxId.current.tap(txId => println(s"still in route1: $txId")) }
      _ <- instrumented(HttpRequest().withUri(checkUri))
      txId4 = TxId.current.tap(txId => println(s"back to route1: $txId"))
      _ <- instrumented(HttpRequest().withUri(checkUri))
      txId5 = TxId.current.tap(txId => println(s"back to route1: $txId"))
    } yield s"""The same TxId should be retained throughout the entire transaction.
         |Instead:
         | - first three gathered TxIds retain the same TxId: $txId1, $txId2, $txId3
         | - the last two (following http requests) revert to default TxId: $txId4, $txId5
         |""".stripMargin
    complete(handling)
  }

  val route2 = path(checkPath) { complete("ok") }

  override protected def routes: Route = route1 ~ route2

  println(
    s"""
       |We check by comparing the hashcode of the current transaction ('TxId' for short).
       |Default transaction hashcode outside transaction bounds: ${TxId.default.value}.
       |Each transaction should receive a unique TxId and retain it to completion. Instead, we
       |see the TxId revert to the default TxId right after calls to `Http().singleRequest`.
       |
       |To test, simply `curl http://$local`
       |""".stripMargin
  )
  startServer(local.host.address, local.port)
}
