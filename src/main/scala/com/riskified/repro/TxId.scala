package com.riskified.repro

import com.newrelic.api.agent.NewRelic

case class TxId(value: Int = NewRelic.getAgent.getTransaction.hashCode) {
  override def toString: String =
    if (this != TxId.default) s"TxId($value)"
    else "TxId(default)"
}

object TxId {
  val default: TxId = TxId()
  def current: TxId = TxId()
}