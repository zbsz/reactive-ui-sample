package com.geteit.react

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget._
import com.geteit.react.service.{Seconds, Millis, PlaybackService}
import com.geteit.util._
import com.geteit.app.ViewHelper
import com.geteit.concurrent.Threading
import com.geteit.events.Signal
import com.geteit.view.{DragGesture, TouchReactor}

class PlaybackView(context: Context, attrs: AttributeSet, style: Int) extends RelativeLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  val service = inject[PlaybackService]                     // inject our service

  inflate(R.layout.playback_view, this, addToRoot = true)   // inflate xml layout

  val tvPosition  : TextView = R.id.tvPosition              // bind text views
  val tvDuration  : TextView = R.id.tvDuration
  val tvSong   : TextView = R.id.tvSong

  val pbProgress  : ProgressBar = R.id.pbProgress           // bind progress bar

  val btnPrevious : ImageButton = R.id.btnPrevious          // bind buttons
  val btnRewind   : ImageButton = R.id.btnRewind
  val btnPlay     : ImageButton = R.id.btnPlay
  val btnForward  : ImageButton = R.id.btnForward
  val btnNext     : ImageButton = R.id.btnNext


  val currentSong = service.currentSong flatMap service.song

  val currentAlbum = currentSong flatMap { song =>
    service.album(song.album)
  }

  val currentPosition = service.currentSong flatMap service.playbackPosition

  val songLabel = for {
    song  <- currentSong
    album <- service.album(song.album)
  } yield s"Song ${album.indexOf(song) + 1} of ${album.songs.size}"

  songLabel.on(Threading.ui) { tvSong.setText }

  currentSong.map(_.duration.toSeconds.toString).on(Threading.ui) { tvDuration.setText }

  val indexInAlbum = for {
    album <- currentAlbum
    song <- currentSong
  } yield 
    (album, album.indexOf(song))

  val prevSong = indexInAlbum map { case (album, index) =>
    if (index <= 0) None
    else Some(album.songs(index - 1))
  }
  
  val nextSong = indexInAlbum map { case (album, index) =>
      if (index >= album.songs.size - 1) None
      else Some(album.songs(index + 1))
  }

  prevSong.map(_.isDefined).on(Threading.ui) { btnPrevious.setEnabled }
  nextSong.map(_.isDefined).on(Threading.ui) { btnNext.setEnabled }

  service.playbackState.map(_.isPlaying).on(Threading.ui) { playing =>
    btnPlay.setImageResource(if (playing) R.drawable.ico_pause else R.drawable.ico_play)
  }

  btnPlay setOnClickListener { v: View =>
    if (service.playbackState.currentValue.exists(_.isPlaying)) service.pause()
    else service.currentSong.currentValue foreach service.play
  }

  btnRewind setOnClickListener { v: View => service.seek(currentPosition.currentValue.fold(Millis(0))(_ - Seconds(30))) }
  btnForward setOnClickListener { v: View => service.seek(currentPosition.currentValue.fold(Millis(0))(_ + Seconds(30))) }
  btnPrevious setOnClickListener { v: View => prevSong.currentValue.flatten foreach { song => service.play(song.uri) } }
  btnNext setOnClickListener { v: View => nextSong.currentValue.flatten foreach { song => service.play(song.uri) } }

  val reactor = new TouchReactor(this)
  val dragGesture = new DragGesture(getContext, reactor)

  def duration = currentSong.currentValue.fold(Millis(0))(_.duration.toMillis)

  def swipeDistance(startX: Float) =
    Signal.wrap(dragGesture.onDrag map {
      case (x, _) => (x - startX) / getWidth
    })

  val dragPosition: Signal[Option[Millis]] = {

    val dragStart = Signal(Option.empty[Float])

    dragGesture.onDragStart { case (x, _) => dragStart ! Some(x) }

    dragGesture.onDragEnd { dragged =>
      if (dragged) dragPosition.currentValue.flatten foreach service.seek
      dragStart ! None
    }

    dragStart flatMap {
      case None => Signal.const(Option.empty[Millis])
      case Some(startX) =>
        val startPos = currentPosition.currentValue.getOrElse(Millis(0))
        swipeDistance(startX) map { diff => Some(startPos + duration * diff) }
    }
  }

  val positionSeconds = dragPosition flatMap {
    case Some(position) => Signal.const(position.toSeconds)
    case None           => currentPosition.map(_.toSeconds)
  }

  positionSeconds.map(_.toString).on(Threading.ui) { tvPosition.setText }

  val progress = for {
    song     <- currentSong
    position <- positionSeconds
  } yield position.seconds * 10000 / song.duration.seconds

  progress.on(Threading.ui) { pbProgress.setProgress }
}
