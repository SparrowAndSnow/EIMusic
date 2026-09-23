package com.eimsound.eimusic.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.util.rememberAutoHideHeaderConnection
import com.eimsound.eimusic.util.rememberAutoHideHeaderState
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.eimsound.eimusic.viewmodel.ConfigViewModel
import com.eimsound.eimusic.viewmodel.SettingViewModel
import com.eimsound.eimusic.viewmodel.UpdateViewModel
import com.eimsound.eimusic.views.components.setting.SettingAbout
import com.eimsound.eimusic.views.components.setting.SettingAccount
import com.eimsound.eimusic.views.components.setting.SettingConfig
import com.eimsound.eimusic.views.components.setting.SettingHeader
import com.eimsound.eimusic.views.components.setting.SettingLanguage
import com.eimsound.eimusic.views.components.setting.SettingLocalPath
import com.eimsound.eimusic.views.components.setting.SettingNotification
import com.eimsound.eimusic.views.components.setting.SettingProxy
import com.eimsound.eimusic.views.components.setting.SettingTheme
import com.eimsound.eimusic.views.components.setting.SettingUpdate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingView(showLoginDialog: Boolean) {
    DisplaySettingView(showLoginDialog)
}

@Composable
expect fun DisplaySettingView(
    showLoginDialog: Boolean,
)

@Composable
fun CommonSettingView(
    showLoginDialog: Boolean,
    displayTheme: Boolean = true,
    displayLanguage: Boolean = true,
    displayProxy: Boolean = true,
    displayLocalPath: Boolean = true
) {
    val settingViewModel = koinViewModel<SettingViewModel>()
    val authViewModel = koinViewModel<AuthViewModel>()
    val updateViewModel = koinViewModel<UpdateViewModel>()
    val configViewModel = koinViewModel<ConfigViewModel>()
    val configState by configViewModel.configState.collectAsState()
    val themeState by settingViewModel.themeState.collectAsState()
    val languageState by settingViewModel.languageState.collectAsState()
    val localPathsState by settingViewModel.localPathsState.collectAsState()
    val proxyState by settingViewModel.proxyState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val notificationState by settingViewModel.notificationState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        SettingContent {
            item(1) {
                SettingAccount(authState, authViewModel::login, authViewModel::logout, showLoginDialog)
            }
            if (displayTheme) item(2) {
                SettingTheme(
                    themeState, settingViewModel::setThemeFollowSystem, settingViewModel::setDarkMode
                )
            }
            item(3){
                SettingNotification(notificationState,
                    settingViewModel::setNotificationEnabled,
                    settingViewModel::setNotificationSoundEnabled
                )
            }
            if (displayLanguage) item(4) {
                SettingLanguage(languageState, settingViewModel::setLanguage)
            }
            if (displayProxy) item(5) {
                SettingProxy(proxyState, settingViewModel::updateProxy)
            }
            item(6) {
                SettingConfig(configState, configViewModel::save)
            }
            if (displayLocalPath) item(7) {
                SettingLocalPath(
                    localPathsState,
                    settingViewModel::onOpenDirectoryPicker,
                    settingViewModel::addLocalPath,
                    settingViewModel::removeLocalPath,
                )
            }
            item(8) {
                SettingUpdate(
                    updateViewModel = updateViewModel,
                    onCheckUpdate = { /* 可选的回调 */ },
                )
            }
            item(9) {
                SettingAbout()
            }

        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
fun SettingContent(content: LazyListScope.() -> Unit) {
    val state = rememberLazyListState()
    val autoHideHeaderState = rememberAutoHideHeaderState()
    val nestedScrollConnection = rememberAutoHideHeaderConnection(state, autoHideHeaderState)
    AnimatedVisibility(
        visible = autoHideHeaderState.isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        SettingHeader()
    }
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize()
            .padding(top = 8.dp)
            .nestedScroll(nestedScrollConnection),
        userScrollEnabled = true,
        flingBehavior = ScrollableDefaults.flingBehavior(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
fun SettingSection(
    title: String, content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
}