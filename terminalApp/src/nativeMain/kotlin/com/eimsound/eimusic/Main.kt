package com.eimsound.eimusic

import com.eimsound.eimusic.di.initKoin
import com.eimsound.eimusic.mpris.createMprisManager
import com.eimsound.eimusic.util.initTuiLogging
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.util.AnyAsciiProcessor
import com.eimsound.eimusic.util.Renderer
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.eimsound.eimusic.viewmodel.ChannelViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.SearchViewModel
import com.jakewharton.mosaic.runMosaicBlocking
import kotlin.system.exitProcess

fun main() {
    initTuiLogging()

    val koinApp = initKoin()
    val koin = koinApp.koin

    val playerViewModel = try {
        koin.get<PlayerViewModel>()
    } catch (e: Exception) {
        println("Failed to init audio engine: ${e.message}")
        koinApp.close()
        return
    }
    val searchViewModel = koin.get<SearchViewModel>()
    val authViewModel = koin.get<AuthViewModel>()
    val playlistViewModel = koin.get<PlaylistViewModel>()
    val localViewModel = koin.get<LocalViewModel>()
    val channelViewModel = koin.get<ChannelViewModel>()
    val controller = try { koin.get<IMediaPlayerController>() } catch (_: Exception) { null }
    val storage = koin.get<IStorage>()
    // Install CJK→ASCII transliteration for terminal display
//    Renderer.install(AnyAsciiProcessor())

    val mpris = controller?.let { createMprisManager(it) }
    mpris?.start()

    val release = {
        mpris?.stop()
        playerViewModel.release()
        searchViewModel.release()
        authViewModel.release()
        playlistViewModel.release()
        localViewModel.release()
        channelViewModel.release()
        koinApp.close()
    }
    val onQuit = {
        release()
        exitProcess(0)
    }

    // 终端进入/退出、光标可见性都由 Mosaic 的 runMosaic* 生命周期负责，这里不再手写 ANSI。
    try {
        runMosaicBlocking(mouse = true) {
            App(
                storage = storage,
                playerViewModel = playerViewModel,
                searchViewModel = searchViewModel,
                authViewModel = authViewModel,
                playlistViewModel = playlistViewModel,
                localViewModel = localViewModel,
                channelViewModel = channelViewModel,
                onQuit = onQuit,
            )
        }
    } catch (e: Exception) {
        println("FATAL: ${e.message}")
        e.printStackTrace()
    } finally {
        release()
    }
}
