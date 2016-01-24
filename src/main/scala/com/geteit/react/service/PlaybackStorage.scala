package com.geteit.react.service

import java.util.concurrent.ConcurrentHashMap

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import com.geteit.events.{EventStream, Signal}
import com.geteit.util.Log._

import scala.collection.JavaConverters._
import scala.concurrent.Future

class PlaybackStorage(context: Context) {
  private implicit val tag: LogTag = "PlaybackStorage"
  import com.geteit.concurrent.Threading.global

  private val songs = new ConcurrentHashMap[Uri, Song]
  private val albums = new ConcurrentHashMap[String, Album]
  private val positions = new ConcurrentHashMap[Uri, Millis]

  private val songChanged = EventStream[Song]()
  private val albumChanged = EventStream[Album]()
  private val positionChanged = EventStream[(Uri, Millis)]()

  reload()

  def firstSong =
    Signal.wrap(albumChanged).orElse(Signal.const(null)) flatMap { _ =>
      verbose(s"first flatMap, albums: $albums")
      if (albums.isEmpty) Signal.empty[Song]
      else albums.values().asScala.maxBy(_.songs.size).songs.headOption.fold(Signal.empty[Song])(Signal.const)
    }

  def song(uri: Uri) =
    Signal.wrap(songChanged.filter(_.uri == uri)).orElse {
      Option(songs.get(uri)).fold(Signal.empty[Song])(Signal.const)
    }

  def album(title: String) =
    Signal.wrap(albumChanged.filter(_.title == title)).orElse {
      Option(albums.get(title)).fold(Signal.empty[Album])(Signal.const)
    }

  def position(uri: Uri) =
    Signal.wrap(positionChanged.filter(_._1 == uri)).map(_._2).orElse {
      Signal.const(Option(positions.get(uri)).getOrElse(Millis(0)))
    }

  def setPosition(uri: Uri, pos: Millis) = {
    positions.put(uri, pos)
    positionChanged ! (uri, pos)
  }

  def reload() = Future {
    val media = listMedia()
    verbose(s"found media: $media")
    media foreach { s =>
      songs.put(s.uri, s)
      songChanged ! s
    }
    media.groupBy(_.album) map {
      case (album, ms) =>
        val a = Album(album, ms.toIndexedSeq)
        albums.put(album, a)
        albumChanged ! a
        a
    }
  }

  private def listMedia() = {
    import MediaStore.Audio._
    import AudioColumns._
    import MediaColumns._

    val cursor = context.getContentResolver.query(Media.EXTERNAL_CONTENT_URI, Array(DATA, ALBUM, TITLE, DURATION), null, null, null)
    try {
      Iterator.continually {
        if (cursor.moveToNext()) Some(Song(Uri.parse(cursor.getString(0)), cursor.getString(1), cursor.getString(2), Millis(cursor.getLong(3))))
        else None
      } .takeWhile(_.isDefined) .map(_.get) .toVector
    } finally {
      cursor.close()
    }
  }
}
