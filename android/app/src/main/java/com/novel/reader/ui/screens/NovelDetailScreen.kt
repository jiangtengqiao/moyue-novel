package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.Chapter
import com.novel.reader.data.model.Novel
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NovelDetailViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _bookmarked = MutableStateFlow(false)
    val bookmarked: StateFlow<Boolean> = _bookmarked

    fun load(novelId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _novel.value = repository.getNovel(novelId)
                _chapters.value = repository.getChapters(novelId)
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    fun toggleBookmark(novelId: String) {
        viewModelScope.launch {
            try {
                repository.toggleBookmark(novelId)
                _bookmarked.value = !_bookmarked.value
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun NovelDetailScreen(
    novelId: String,
    viewModel: NovelDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onRead: (String, Int) -> Unit,
    onChapterList: (String) -> Unit,
) {
    LaunchedEffect(novelId) { viewModel.load(novelId) }

    val novel by viewModel.novel.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val bookmarked by viewModel.bookmarked.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (loading) {
            LoadingIndicator()
            return@Box
        }

        val n = novel ?: return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleBookmark(novelId) }) {
                    Icon(
                        if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        "收藏",
                        tint = if (bookmarked) AccentGold else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 小说信息
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(AccentGoldLight.copy(alpha = 0.6f), AccentGoldDark.copy(alpha = 0.3f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = n.title.take(2),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        color = InkBlack,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = n.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text = n.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        InfoChip("${n.wordCount / 10000}万字")
                        Spacer(Modifier.width(8.dp))
                        InfoChip(if (n.status == "ongoing") "连载中" else "已完结")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        InfoChip("${n.viewCount}阅读")
                        Spacer(Modifier.width(8.dp))
                        InfoChip("${n.collectCount}收藏")
                    }
                }
            }

            // 简介
            Spacer(Modifier.height(20.dp))
            Text(
                text = "简介",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = n.description ?: "暂无简介",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // 标签
            if (n.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    n.tags.take(5).forEach { tag ->
                        TagChip(tag)
                    }
                }
            }

            // 章节预览
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (chapters.size > 3) {
                    Text(
                        "全部${chapters.size}章",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentGold,
                        modifier = Modifier.clickable { onChapterList(novelId) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            chapters.take(5).forEach { ch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRead(novelId, ch.sortOrder) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = ch.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (ch != chapters.take(5).last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
            Spacer(Modifier.height(80.dp))
        }

        // 底部阅读按钮
        BottomBar(
            novelId = novelId,
            chapterCount = chapters.size,
            onRead = { onRead(novelId, 0) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BottomBar(novelId: String, chapterCount: Int, onRead: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onRead,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = PaperWhite),
                enabled = chapterCount > 0,
            ) {
                Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (chapterCount > 0) "开始阅读" else "暂无章节")
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun TagChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = AccentGold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AccentGold.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
