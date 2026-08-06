package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novel.reader.data.model.Creator
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.data.repository.SessionManager
import com.novel.reader.ui.Routes
import com.novel.reader.ui.components.StatCard
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatorViewModel @Inject constructor(
    private val repository: NovelRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _creator = MutableStateFlow<Creator?>(null)
    val creator: StateFlow<Creator?> = _creator

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init { checkStatus() }

    fun checkStatus() {
        viewModelScope.launch {
            val token = sessionManager.tokenFlow.first()
            _isLoggedIn.value = token != null
            if (token != null) {
                try { _creator.value = repository.getCreatorProfile() } catch (_: Exception) {}
            }
            _loading.value = false
        }
    }
}

@Composable
fun CreatorScreen(
    viewModel: CreatorViewModel = hiltViewModel(),
    navController: NavController,
) {
    val creator by viewModel.creator.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) { viewModel.checkStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "创作中心",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        )

        if (!isLoggedIn) {
            // 未登录
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = InkLight, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("登录后开启创作之旅", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Routes.LOGIN) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = PaperWhite),
                ) { Text("去登录") }
            }
        } else if (creator == null) {
            // 已登录但未成为创作者
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(AccentGoldLight.copy(alpha = 0.3f), AccentGoldDark.copy(alpha = 0.1f))))
                    .padding(24.dp),
            ) {
                Text(
                    "成为创作者",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "注册成为墨阅创作者，发布你的原创作品。\n支持 TXT、Markdown、DOCX 批量导入，\n文件夹一键上传，自动解析章节。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Routes.CREATOR_REGISTER) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = PaperWhite),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("立即注册") }
            }
        } else {
            // 已是创作者 - 显示仪表盘入口
            val c = creator!!
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(20.dp),
            ) {
                Text(c.penName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (c.verified) {
                    Text("已认证创作者", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatCard("作品", c.totalNovels.toString())
                    StatCard("总字数", formatWords(c.totalWords))
                    StatCard("读者", formatReaders(c.totalReaders))
                }
            }

            Spacer(Modifier.height(20.dp))

            // 功能入口
            CreatorMenuItem("数据概览", Icons.Outlined.Insights) { navController.navigate(Routes.CREATOR_DASHBOARD) }
            CreatorMenuItem("发布新作品", Icons.Outlined.PostAdd) { navController.navigate(Routes.CREATE_NOVEL) }
            CreatorMenuItem("我的作品", Icons.Outlined.LibraryBooks) { navController.navigate(Routes.MY_NOVELS) }
            CreatorMenuItem("创作指南", Icons.Outlined.HelpOutline) { }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun CreatorMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
}

private fun formatWords(n: Int): String = if (n > 10000) "${n / 10000}万" else n.toString()
private fun formatReaders(n: Int): String = if (n > 10000) "${n / 10000}万" else n.toString()
