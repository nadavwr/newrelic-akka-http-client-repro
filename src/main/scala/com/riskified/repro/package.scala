package com.riskified

package object repro {

  implicit class Tap[A](thunk: => A) {
    def tap(f: A => Any): A = {
      val result = thunk
      f(result)
      result
    }
  }

}
