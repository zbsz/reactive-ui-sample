package com.geteit.react

import android.app.Activity
import android.os.Bundle
import com.geteit.app.ActivityHelper

class MainActivity extends Activity with ActivityHelper {

  lazy val playback: PlaybackView = R.id.playbackView

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.main)
  }
}


