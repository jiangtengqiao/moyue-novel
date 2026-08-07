package com.novel.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
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
import com.novel.reader.data.model.Chapter
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.components.LoadingIndicator
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterListViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _isCreator = MutableStateFlow(false)
    val isCreator: StateFlow<Boolean> = _isCreator

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load(novelId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _chapters.value = repository.getChapters(novelId)
                try { repository.getCreatorProfile(); _isCreator.value = true } catch (_: Exception) { _isCreator.value = false }
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    fun setQuery(q: String) { _query.value = q }

    fun deleteChapter(novelId: String, chapter: Chapter) {
        viewModelScope.launch {
            try {
                repository.deleteChapter(novelId, chapter.id)
                _chapters.value = _chapters.value.filter { it.id != chapter.id }
                _message.value = "已删除: ${chapter.title}"
            } catch (e: Exception) { _message.value = "删除失败: ${e.message}" }
        }
    }
}

@Composable
fun ChapterListScreen(
    novelId: String,
    viewModel: ChapterListViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChapterClick: (String, Int) -> Unit,
    onEditChapter: (String, String?) -> Unit,
    onAddChapter: (String) -> Unit,
) {
    LaunchedEffect(novelId) { viewModel.load(novelId) }

    val chapters by viewModel.chapters.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val query by viewModel.query.collectAsState()
    val isCreator by viewModel.isCreator.collectAsState()
    val message by viewModel.message.collectAsState()

    val filtered = remember(chapters, query) {
        if (query.isBlank()) chapters
        else chapters.filter { it.title.contains(query, ignoreCase = true) }
    }

    // 按卷分组：volume 为空时归入"正文"
    val grouped = remember(filtered) {
        filtered.groupBy { it.volume.ifBlank { "正文" } }
    }

    val expandedVolumes = remember { mutableStateMapOf<String, Boolean>().apply { putAll(grouped.keys.map { it to true }) } }

    LaunchedEffect(grouped.keys) {
        grouped.keys.forEach { k -> if (!expandedVolumes.contains(k)) expandedVolumes[k] = true }
    }

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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
            Text("目录", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
            Spacer(Modifier.weight(1f))
            Text("共${chapters.size}章", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isCreator) {
                IconButton(onClick = { onAddChapter(novelId) }) {
                    Icon(Icons.Outlined.Add, "添加章节", tint = AccentGold)
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("搜索章节") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        message?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (it.startsWith("已删除")) StatusSuccess else MaterialTheme.colorScheme.error)
        }

        if (loading) {
            LoadingIndicator()
        } else if (filtered.isEmpty()) {
            com.novel.reader.ui.components.EmptyState(if (query.isBlank()) "暂无章节" else "未找到匹配章节")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                grouped.forEach { (volume, chs) ->
                    item(key = "vol_$volume") {
                        val expanded = expandedVolumes[volume] ?: true
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ) {
                            Row(
                                modifier = Modifier.clickable { expandedVolumes[volume] = !expanded }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "卷·$volume",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentGold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("${chs.size}章", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (expanded) "折叠" else "展开",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    if (expandedVolumes[volume] != false) {
                        items(chs) { ch ->
                            ChapterItemRow(
                                chapter = ch,
                                isCreator = isCreator,
                                onClick = { onChapterClick(novelId, ch.sortOrder) },
                                onEdit = { onEditChapter(novelId, ch.id) },
                                onDelete = { viewModel.deleteChapter(novelId, ch) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterItemRow(
    chapter: Chapter,
    isCreator: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showActions by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { showActions = !showActions }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${chapter.sortOrder + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = chapter.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).clickable { onClick() },
        )
        if (isCreator && showActions) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        } else {
            Text(
                text = "${chapter.wordCount}字",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
