package com.eimsound.eimusic

import com.eimsound.eimusic.db.DbSessionRepository
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
import com.jakewharton.mosaic.runMosaic
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    initTuiLogging()

    val koinApp = initKoin()
    val koin = koinApp.koin
    // 启动时打开 SQLite（schema/migration），避免首次查询才建文件。
    koin.get<com.eimsound.eimusic.db.DbPlaylistRepository>()

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
    val sessionRepository = koin.get<DbSessionRepository>()
    val initialTab = sessionRepository.load().lastTab
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
    // 退出不能走 exitProcess：那会跳过 Mosaic 的 withTerminal/withFinalizationHook，
    // 备用屏、鼠标和 raw mode 都来不及恢复。改为取消 composition 所在协程，让库正常收尾。
    var quitJob: Job? = null
    val requestQuit: () -> Unit = { quitJob?.cancel() }

    try {
        runBlocking {
            val job = launch {
                runMosaic(mouse = true, alternateScreen = true) {
                    App(
                        storage = storage,
                        initialTab = initialTab,
                        sessionRepository = sessionRepository,
                        playerViewModel = playerViewModel,
                        searchViewModel = searchViewModel,
                        authViewModel = authViewModel,
                        playlistViewModel = playlistViewModel,
                        localViewModel = localViewModel,
                        channelViewModel = channelViewModel,
                        onQuit = requestQuit,
                    )
                }
            }
            quitJob = job
            job.join()
        }
    } catch (e: Exception) {
        println("FATAL: ${e.message}")
        e.printStackTrace()
    } finally {
        release()
    }
}
