package com.geteit.react.service

import java.util.concurrent.TimeUnit

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import com.geteit.concurrent.{CancellableFuture, LimitedExecutionContext}
import com.geteit.events.{EventContext, Signal}
import com.geteit.inject.{Injector, Injectable}
import com.geteit.react.service.PlaybackState.{Paused, Playing, Stopped}
import com.geteit.util.Log._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class ChapterId(str: String)

case class BookId(str: String)

trait Duration {
  def millis: Long
  def seconds: Int = (millis / 1000).toInt

  def toMillis = Millis(millis)
  def toSeconds = Seconds(seconds)
  def +(d: Duration) = Millis(millis + d.millis)
  def -(d: Duration) = Millis(millis - d.millis)
  def *(m: Float) = Millis((millis * m).toLong)
}

case class Millis(millis: Long) extends Duration

case class Seconds(override val seconds: Int) extends Duration {
  override def millis: Long = seconds * 1000L

  override def toString: String = f"${seconds / 60}%02d:${seconds % 60}%02d"
}

case class Song(uri: Uri, album: String, title: String, duration: Duration)

case class Album(title: String, songs: IndexedSeq[Song]) {
  def indexOf(s: Song) = songs.indexOf(s)
}

sealed trait PlaybackState {
  def isPlaying: Boolean
}
object PlaybackState {

  case object Playing extends PlaybackState {
    override def isPlaying: Boolean = true
  }
  case object Paused extends PlaybackState {
    override def isPlaying: Boolean = false
  }
  case object Stopped extends PlaybackState {
    override def isPlaying: Boolean = false
  }
}


class PlaybackService(context: Context)(implicit ec: EventContext, inj: Injector) extends Injectable {
  private implicit val dispatcher = new LimitedExecutionContext()
  private val player = new MediaPlayer()
  private val storage = inject[PlaybackStorage]

  // Currently played song uri
  val currentSong = Signal[Uri]()
  val playbackState = Signal[PlaybackState](Stopped)

  storage.firstSong { s =>
    if (currentSong.currentValue.isEmpty) currentSong ! s.uri
  }

  player.setOnCompletionListener(new OnCompletionListener {
    override def onCompletion(mp: MediaPlayer): Unit = playbackState ! Stopped
  })

  def playbackPosition(song: Uri): Signal[Millis] = storage.position(song)
  def song(uri: Uri): Signal[Song] = storage.song(uri)
  def album(title: String): Signal[Album] = storage.album(title)

  def play(uri: Uri) = Future {
    if (currentSong.currentValue.contains(uri) && playbackState.currentValue.contains(Paused)) {
      playbackState ! Playing
      player.start()
    } else {
      playbackState ! Playing
      currentSong ! uri
      player.reset()
      player.setDataSource(context, uri)
      player.prepare()
      player.start()
    }

    reportPosition()
  }

  private def reportPosition(): Unit =
    if (playbackState.currentValue.contains(Playing)) {
      currentSong.currentValue foreach { uri => storage.setPosition(uri, Millis(player.getCurrentPosition)) }
      CancellableFuture.delayed(FiniteDuration(100, TimeUnit.MILLISECONDS))(reportPosition())
    }

  def pause() = Future {
    if (playbackState.currentValue.contains(Playing)) {
      player.pause()
      playbackState ! Paused
    }
  }

  def seek(position: Duration) = Future {
    if (playbackState.currentValue.contains(Playing) || playbackState.currentValue.contains(Paused)) {
      player.seekTo(position.millis.toInt)
      currentSong.currentValue foreach { uri => storage.setPosition(uri, position.toMillis) }
    }
  }
}
