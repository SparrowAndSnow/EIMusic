package com.eimsound.eimusic

import com.eimsound.eimusic.di.initKoin
import com.eimsound.eimusic.mpris.createMprisManager
import com.eimsound.eimusic.util.TuiLogger
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
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.system.exitProcess

fun main() {
    val logFile = "/tmp/eimusic.log"
    TuiLogger.fileWriter = { line ->
        runCatching {
            SystemFileSystem.sink(Path(logFile), append = true).buffered().use { sink ->
                sink.write((line + "\n").encodeToByteArray())
                sink.flush()
            }
        }
    }
    TuiLogger.log("App started")
    TuiLogger.log("Tip: 终端图标需要 Nerd Font 支持 (https://www.nerdfonts.com/font-downloads)")
    TuiLogger.log("      安装后设为终端字体即可显示  等控制图标")
    TuiLogger.log("Tip: 专辑封面需要 chafa 支持 (https://hpjansson.org/chafa/)")
    TuiLogger.log("      Arch: sudo pacman -S chafa")
    TuiLogger.log("      Debian/Ubuntu: sudo apt install chafa")
    TuiLogger.log("      Fedora: sudo dnf install chafa")
    TuiLogger.log("      macOS: brew install chafa")
    TuiLogger.log("      Windows: winget install chafa")
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
        print("[?25h[?1049l")
        release()
        exitProcess(0)
    }

    print("[?1049h[?25l")

    try {
        runMosaicBlocking {
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
        print("[?25h[?1049l")
        println("FATAL: ${e.message}")
        e.printStackTrace()
    } finally {
        print("[?25h[?1049l")
        release()
    }
}
