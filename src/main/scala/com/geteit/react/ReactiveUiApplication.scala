package com.geteit.react

import com.geteit.app.GtApplication
import com.geteit.inject.{GtAppModule, GtModule, Module}
import com.geteit.react.service.PlaybackService

class ReactiveUiApplication extends GtApplication {

  override lazy val module = AppModule :: GtAppModule() :: GtModule

  val AppModule = Module { implicit bind =>
    bind [PlaybackService] to new PlaybackService(getApplicationContext)
  }
}
