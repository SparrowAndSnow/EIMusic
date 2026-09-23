package com.eimsound.eimusic.mpris

import com.eimsound.eimusic.playback.IMediaPlayerController

expect fun createMprisManager(controller: IMediaPlayerController): MprisManager?
