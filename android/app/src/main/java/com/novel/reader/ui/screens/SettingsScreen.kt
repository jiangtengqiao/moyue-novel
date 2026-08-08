package com.novel.reader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novel.reader.Constants
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.data.repository.SessionManager
import com.novel.reader.ui.Routes
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: NovelRepository,
) : ViewModel() {
    private val _cacheCleared = MutableStateFlow(false)
    val cacheCleared: StateFlow<Boolean> = _cacheCleared

    private val _updateMsg = MutableStateFlow<String?>(null)
    val updateMsg: StateFlow<String?> = _updateMsg

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking

    private val _logoutDone = MutableStateFlow(false)
    val logoutDone: StateFlow<Boolean> = _logoutDone

    fun clearCache(context: android.content.Context) {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                _cacheCleared.value = true
            } catch (_: Exception) {}
        }
    }

    fun checkUpdate() {
        viewModelScope.launch {
            _checking.value = true
            _updateMsg.value = null
            try {
                val resp = repository.checkUpdate(Constants.APP_VERSION)
                if (resp.hasUpdate && resp.latestVersion != null) {
                    _updateMsg.value = "发现新版本: ${resp.latestVersion.versionName}\n${resp.latestVersion.updateLog}"
                } else {
                    _updateMsg.value = "已是最新版本"
                }
            } catch (_: Exception) {
                _updateMsg.value = "检查更新失败，请稍后重试"
            }
            _checking.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _logoutDone.value = true
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController,
) {
    val context = LocalContext.current
    val cacheCleared by viewModel.cacheCleared.collectAsState()
    val updateMsg by viewModel.updateMsg.collectAsState()
    val checking by viewModel.checking.collectAsState()
    val logoutDone by viewModel.logoutDone.collectAsState()

    LaunchedEffect(logoutDone) {
        if (logoutDone) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.HOME) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
            Text("设置", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // 关于
            SectionTitle("关于")
            SettingsCard(
                icon = Icons.Outlined.Info,
                title = "版本",
                subtitle = "墨阅小说 v${Constants.APP_VERSION} (${Constants.APP_VERSION_CODE})",
            )
            Spacer(Modifier.height(8.dp))
            SettingsCard(
                icon = Icons.Outlined.Code,
                title = "开源仓库",
                subtitle = "github.com/jiangtengqiao/moyue-novel",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jiangtengqiao/moyue-novel"))
                    context.startActivity(intent)
                },
            )

            Spacer(Modifier.height(24.dp))

            // 阅读
            SectionTitle("阅读")
            SettingsCard(
                icon = Icons.Outlined.History,
                title = "阅读历史",
                subtitle = "查看最近阅读记录",
                onClick = { navController.navigate(Routes.READING_HISTORY) },
            )

            Spacer(Modifier.height(24.dp))

            // 更新
            SectionTitle("更新")
            SettingsCard(
                icon = Icons.Outlined.SystemUpdate,
                title = "检查更新",
                subtitle = if (checking) "检查中..." else "检查是否有新版本",
                onClick = { viewModel.checkUpdate() },
            )
            updateMsg?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 数据
            SectionTitle("数据")
            SettingsCard(
                icon = Icons.Outlined.CleaningServices,
                title = "清除缓存",
                subtitle = if (cacheCleared) "缓存已清除" else "清理临时文件",
                onClick = { viewModel.clearCache(context) },
            )

            Spacer(Modifier.height(24.dp))

            // 反馈
            SectionTitle("反馈")
            SettingsCard(
                icon = Icons.Outlined.BugReport,
                title = "问题反馈",
                subtitle = "提交 Bug 或建议",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:moyue-novel@outlook.com")
                        putExtra(Intent.EXTRA_SUBJECT, "墨阅小说 - 问题反馈")
                    }
                    context.startActivity(intent)
                },
            )
            Spacer(Modifier.height(8.dp))
            SettingsCard(
                icon = Icons.Outlined.Star,
                title = "给我们评分",
                subtitle = "支持一下开发者",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jiangtengqiao/moyue-novel"))
                    context.startActivity(intent)
                },
            )

            Spacer(Modifier.height(24.dp))

            // 法律与协议
            SectionTitle("法律与协议")
            SettingsCard(
                icon = Icons.Outlined.Description,
                title = "用户服务协议",
                subtitle = "查看完整服务条款",
                onClick = { navController.navigate(Routes.agreement("user")) },
            )
            Spacer(Modifier.height(8.dp))
            SettingsCard(
                icon = Icons.Outlined.PrivacyTip,
                title = "隐私政策",
                subtitle = "了解我们如何保护您的隐私",
                onClick = { navController.navigate(Routes.agreement("privacy")) },
            )
            Spacer(Modifier.height(8.dp))
            SettingsCard(
                icon = Icons.Outlined.Group,
                title = "社区规范",
                subtitle = "共同维护社区秩序",
                onClick = { navController.navigate(Routes.agreement("community")) },
            )
            Spacer(Modifier.height(8.dp))
            SettingsCard(
                icon = Icons.Outlined.Copyright,
                title = "版权声明",
                subtitle = "版权保护与侵权处理",
                onClick = { navController.navigate(Routes.agreement("copyright")) },
            )

            Spacer(Modifier.height(24.dp))

            // 账号
            SectionTitle("账号")
            SettingsCard(
                icon = Icons.Outlined.Logout,
                title = "退出登录",
                subtitle = "退出当前账号",
                onClick = { viewModel.logout() },
            )

            Spacer(Modifier.height(48.dp))
            Text(
                text = "墨阅小说 MoYue Novel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = AccentGold,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick?.invoke() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentGold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
    }
}
