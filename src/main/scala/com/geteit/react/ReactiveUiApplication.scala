package com.geteit.react

import com.geteit.app.GtApplication
import com.geteit.events.EventContext
import com.geteit.inject.{GtModule, Module}
import com.geteit.react.service.{PlaybackService, PlaybackStorage}

class ReactiveUiApplication extends GtApplication {

  override lazy val module = AppModule :: GtModule

  val AppModule = Module { implicit bind =>
    bind [PlaybackStorage] to new PlaybackStorage(getApplicationContext)
    bind [PlaybackService] to new PlaybackService(getApplicationContext)(EventContext.Global, bind)
  }
}
