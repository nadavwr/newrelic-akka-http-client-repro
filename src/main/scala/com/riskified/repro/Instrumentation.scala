package com.riskified.repro

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.newrelic.api.agent.{NewRelic, Token, Trace}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Instrumentation {
  def instrumentSingleRequest(
      send: HttpRequest => Future[HttpResponse]
  )(implicit ec: ExecutionContext): HttpRequest => Future[HttpResponse] = {
    @Trace(async = true)
    def link(token: Token)(triedResponse: Try[HttpResponse]) = {
      token.linkAndExpire()
      Future.unit.transform(_ => triedResponse)
    }
    request =>
      val token = NewRelic.getAgent.getTransaction.getToken
      send(request).transformWith(link(token))
  }

}
