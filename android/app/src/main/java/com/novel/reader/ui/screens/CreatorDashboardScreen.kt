package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.DashboardData
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.components.LoadingIndicator
import com.novel.reader.ui.components.StatCard
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _data = MutableStateFlow<DashboardData?>(null)
    val data: StateFlow<DashboardData?> = _data

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try { _data.value = repository.getDashboard() } catch (_: Exception) {}
            _loading.value = false
        }
    }
}

@Composable
fun CreatorDashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onCreateNovel: () -> Unit,
    onMyNovels: () -> Unit,
    onBack: () -> Unit,
) {
    val data by viewModel.data.collectAsState()
    val loading by viewModel.loading.collectAsState()

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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
            Text("数据概览", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }

        if (loading) {
            LoadingIndicator(Modifier.height(300.dp))
        } else if (data != null) {
            val d = data!!
            // 统计卡片网格
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCard("作品总数", d.totalNovels.toString())
                    StatCard("总字数", format(d.totalWords))
                    StatCard("总章节", d.totalChapters.toString())
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCard("总阅读", format(d.totalViews))
                    StatCard("总点赞", format(d.totalLikes))
                    StatCard("总收藏", format(d.totalCollects))
                }
            }

            Spacer(Modifier.height(24.dp))

            // 快捷操作
            Text("快捷操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            QuickActionItem("发布新作品", Icons.Outlined.PostAdd, onCreateNovel)
            QuickActionItem("管理作品", Icons.Outlined.LibraryBooks, onMyNovels)

            Spacer(Modifier.height(24.dp))

            // 最近作品
            if (d.recentNovels.isNotEmpty()) {
                Text("最近作品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                d.recentNovels.forEach { novel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(novel.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text("${novel.wordCount / 10000}万字  ${novel.chapterCount}章", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${novel.viewCount}阅读", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun QuickActionItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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

private fun format(n: Int): String = when {
    n > 100000000 -> "${n / 100000000}亿"
    n > 10000 -> "${n / 10000}万"
    else -> n.toString()
}
