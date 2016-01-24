package com.geteit.react

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.geteit.app.ActivityHelper
import com.geteit.react.service.PlaybackStorage

class MainActivity extends AppCompatActivity with ActivityHelper {

  lazy val storage = inject[PlaybackStorage]

  lazy val playback: PlaybackView = R.id.playbackView

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.main)

    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, Array(android.Manifest.permission.READ_EXTERNAL_STORAGE), 0)
    }
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]) = {
    storage.reload()
  }
}


