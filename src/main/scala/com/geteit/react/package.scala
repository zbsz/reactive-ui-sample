package com.geteit

import android.content.Context
import android.view.{LayoutInflater, ViewGroup, View}
import com.geteit.react.service.{Seconds, Millis}
import com.geteit.util.Log

package object react {

  def inflate[A <: View](resource: Int, root: ViewGroup, addToRoot: Boolean = false)(implicit context: Context): A =
    try {
      LayoutInflater.from(context).inflate(resource, root, addToRoot).asInstanceOf[A]
    } catch {
      case e: Throwable =>
        Log.error("inflate failed", e)("com.geteit.react")
        throw e
    }

  implicit class RichInt(v: Int) {
    def seconds = Seconds(v)
    def millis = Millis(v)
  }
}
