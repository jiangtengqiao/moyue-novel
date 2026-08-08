package com.novel.reader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowUp
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
import com.novel.reader.data.model.VersionHistoryItem
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

    // 检测到的最新版本信息（用于折叠展示）
    private val _latestVersion = MutableStateFlow<VersionHistoryItem?>(null)
    val latestVersion: StateFlow<VersionHistoryItem?> = _latestVersion

    // 历史版本列表（折叠日志）
    private val _versionHistory = MutableStateFlow<List<VersionHistoryItem>>(emptyList())
    val versionHistory: StateFlow<List<VersionHistoryItem>> = _versionHistory

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError

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
            _latestVersion.value = null
            try {
                val resp = repository.checkUpdate(Constants.APP_VERSION)
                if (resp.hasUpdate && resp.latestVersion != null) {
                    val v = resp.latestVersion
                    _latestVersion.value = VersionHistoryItem(
                        versionName = v.versionName,
                        versionCode = v.versionCode,
                        updateTitle = v.updateTitle,
                        updateLog = v.updateLog,
                        forceUpdate = v.forceUpdate,
                        isActive = true,
                        createdAt = v.createdAt,
                    )
                    _updateMsg.value = "发现新版本 v${v.versionName}"
                } else {
                    _updateMsg.value = "已是最新版本"
                }
            } catch (_: Exception) {
                _updateMsg.value = "检查更新失败，请稍后重试"
            }
            _checking.value = false
        }
    }

    fun loadVersionHistory() {
        viewModelScope.launch {
            _historyLoading.value = true
            _historyError.value = null
            try {
                val resp = repository.getVersionHistory()
                _versionHistory.value = resp.versions
            } catch (_: Exception) {
                _historyError.value = "加载更新日志失败"
            }
            _historyLoading.value = false
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
    val latestVersion by viewModel.latestVersion.collectAsState()
    val versionHistory by viewModel.versionHistory.collectAsState()
    val historyLoading by viewModel.historyLoading.collectAsState()
    val historyError by viewModel.historyError.collectAsState()

    // 折叠状态：检测到的新版本详情
    var latestExpanded by remember { mutableStateOf(false) }
    // 折叠状态：历史日志整体展开
    var historyExpanded by remember { mutableStateOf(false) }
    // 折叠状态：每个历史版本是否展开
    val expandedVersions = remember { mutableStateMapOf<String, Boolean>() }

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
                subtitle = if (checking) "检查中..." else "当前版本 v${Constants.APP_VERSION}",
                onClick = { viewModel.checkUpdate() },
            )

            // 检查结果提示
            updateMsg?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (latestVersion != null) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            // 新版本折叠详情卡片
            latestVersion?.let { v ->
                Spacer(Modifier.height(8.dp))
                UpdateLogCard(
                    versionName = v.versionName,
                    versionCode = v.versionCode,
                    title = v.updateTitle ?: "更新内容",
                    log = v.updateLog,
                    forceUpdate = v.forceUpdate,
                    createdAt = v.createdAt,
                    expanded = latestExpanded,
                    onToggle = { latestExpanded = !latestExpanded },
                    highlight = true,
                )
            }

            Spacer(Modifier.height(8.dp))
            // 更新日志入口（折叠展开历史版本列表）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (!historyExpanded && versionHistory.isEmpty()) {
                            viewModel.loadVersionHistory()
                        }
                        historyExpanded = !historyExpanded
                    },
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
                        Icon(Icons.Outlined.History, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("更新日志", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (historyExpanded) "收起历史版本" else "查看历史版本更新记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        if (historyExpanded) Icons.AutoMirrored.Outlined.KeyboardArrowUp else Icons.AutoMirrored.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 历史版本折叠列表
            AnimatedVisibility(
                visible = historyExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (historyLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("加载中...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    historyError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    versionHistory.forEach { item ->
                        val key = "${item.versionCode}_${item.versionName}"
                        UpdateLogCard(
                            versionName = item.versionName,
                            versionCode = item.versionCode,
                            title = item.updateTitle ?: "更新内容",
                            log = item.updateLog,
                            forceUpdate = item.forceUpdate,
                            createdAt = item.createdAt,
                            expanded = expandedVersions[key] ?: false,
                            onToggle = {
                                expandedVersions[key] = !(expandedVersions[key] ?: false)
                            },
                            highlight = false,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (!historyLoading && versionHistory.isEmpty() && historyError == null) {
                        Text(
                            text = "暂无更新记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
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

/**
 * 可折叠的更新日志卡片
 * @param highlight 是否高亮（最新检测到的版本）
 */
@Composable
private fun UpdateLogCard(
    versionName: String,
    versionCode: Int,
    title: String,
    log: String,
    forceUpdate: Boolean,
    createdAt: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    highlight: Boolean = false,
) {
    val cardColor = if (highlight) AccentGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val borderColor = if (highlight) AccentGold.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        color = cardColor,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // 标题行：版本号 + 强制更新标签 + 折叠箭头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "v$versionName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) AccentGold else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(build $versionCode)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (forceUpdate) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "强制更新",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.AutoMirrored.Outlined.KeyboardArrowUp else Icons.AutoMirrored.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // 时间 + 标题副行
            createdAt?.let { time ->
                val displayTime = if (time.length >= 10) time.substring(0, 10) else time
                Text(
                    text = "$displayTime · $title",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } ?: run {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // 折叠内容：完整更新日志
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val logContent = log.trim().ifEmpty { "暂无更新说明" }
                val logLines = logContent.split("\n")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                ) {
                    logLines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentGold,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text(
                                    text = trimmed,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
