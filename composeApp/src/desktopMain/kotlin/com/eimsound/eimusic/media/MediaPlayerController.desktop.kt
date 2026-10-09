package com.eimsound.eimusic.media

import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController

/**
 * 桌面平台的媒体播放控制器实现，全部行为委托给 [JavaStreamPlayerController]。
 */
actual class MediaPlayerController actual constructor(
    trackUriResolverFactory: TrackUriResolverFactory
) : JavaStreamPlayerController(trackUriResolverFactory), IMediaPlayerController
