package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.novel.reader.data.model.NovelBrief
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.components.EmptyState
import com.novel.reader.ui.components.LoadingIndicator
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyNovelsViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _novels = MutableStateFlow<List<NovelBrief>>(emptyList())
    val novels: StateFlow<List<NovelBrief>> = _novels

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repository.getCreatorNovels()
                _novels.value = resp.items
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    fun deleteNovel(novel: NovelBrief) {
        viewModelScope.launch {
            try {
                repository.deleteNovel(novel.id)
                _novels.value = _novels.value.filter { it.id != novel.id }
                _message.value = "已删除：${novel.title}"
            } catch (e: Exception) { _message.value = "删除失败：${e.message}" }
        }
    }
}

@Composable
fun MyNovelsScreen(
    viewModel: MyNovelsViewModel = hiltViewModel(),
    onNovelClick: (String) -> Unit,
    onUpload: (String) -> Unit,
    onEditNovel: (String) -> Unit,
    onBack: () -> Unit,
) {
    val novels by viewModel.novels.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
            Text("我的作品", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
        }

        message?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (it.startsWith("已删除")) StatusSuccess else MaterialTheme.colorScheme.error)
        }

        if (loading) {
            LoadingIndicator()
        } else if (novels.isEmpty()) {
            EmptyState("还没有作品\n去创建你的第一部作品吧", Modifier.fillMaxSize())
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(novels) { novel ->
                    var showActions by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).clickable { onNovelClick(novel.id) }) {
                                    Text(novel.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${novel.author}  |  ${novel.wordCount / 10000}万字  |  ${novel.chapterCount}章", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${novel.viewCount}阅读  ${novel.rating}评分", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                                }
                                IconButton(onClick = { showActions = !showActions }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                                }
                            }
                            if (showActions) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(onClick = { onUpload(novel.id) }, label = { Text("上传章节") }, leadingIcon = { Icon(Icons.Outlined.Upload, null, modifier = Modifier.size(16.dp)) })
                                    AssistChip(onClick = { onEditNovel(novel.id) }, label = { Text("编辑信息") }, leadingIcon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp)) })
                                    AssistChip(
                                        onClick = { viewModel.deleteNovel(novel) },
                                        label = { Text("删除") },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
