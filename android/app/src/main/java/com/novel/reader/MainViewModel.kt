package com.novel.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.Announcement
import com.novel.reader.data.model.UpdateCheckResponse
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.data.repository.SessionManager
import com.novel.reader.service.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashState {
    object Loading : SplashState()
    object GoToMain : SplashState()
    object GoToLogin : SplashState()
}

data class UpdateInfo(
    val response: UpdateCheckResponse,
    val announcements: List<Announcement>,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NovelRepository,
    private val sessionManager: SessionManager,
    private val updateManager: UpdateManager,
) : ViewModel() {

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading

    init {
        checkStartup()
    }

    private fun checkStartup() {
        viewModelScope.launch {
            try {
                // 检查登录状态
                val token = sessionManager.tokenFlow.first()
                val announcements = try {
                    repository.getLatestAnnouncements()
                } catch (_: Exception) { emptyList() }

                // 检查更新
                val updateResp = try {
                    repository.checkUpdate(Constants.APP_VERSION)
                } catch (_: Exception) {
                    UpdateCheckResponse(hasUpdate = false, message = "")
                }

                _updateInfo.value = UpdateInfo(updateResp, announcements)

                // 根据登录状态决定跳转
                _splashState.value = if (token != null) SplashState.GoToMain else SplashState.GoToMain
            } catch (_: Exception) {
                _splashState.value = SplashState.GoToMain
            }
        }
    }

    fun downloadAndInstall() {
        val info = _updateInfo.value?.response?.latestVersion ?: return
        viewModelScope.launch {
            _downloading.value = true
            try {
                val apkFile = updateManager.downloadApk { progress ->
                    _downloadProgress.value = progress
                }
                _downloading.value = false
                updateManager.installApk(apkFile)
            } catch (_: Exception) {
                _downloading.value = false
            }
        }
    }

    fun skipUpdate() {
        _splashState.value = SplashState.GoToMain
    }
}
