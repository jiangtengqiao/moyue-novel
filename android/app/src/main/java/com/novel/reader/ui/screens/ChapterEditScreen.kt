package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterEditViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done

    fun save(novelId: String, chapterId: String?, title: String, content: String, level: Int, volume: String) {
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                if (chapterId == null) {
                    repository.createChapter(novelId, title, content, level, volume)
                } else {
                    repository.updateChapter(novelId, chapterId, title, content, level, volume)
                }
                _done.value = true
            } catch (e: Exception) { _error.value = "保存失败: ${e.message}" }
            _saving.value = false
        }
    }
}

@Composable
fun ChapterEditScreen(
    novelId: String,
    chapterId: String? = null,
    initialTitle: String = "",
    initialContent: String = "",
    initialLevel: Int = 2,
    initialVolume: String = "",
    viewModel: ChapterEditViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var volume by remember { mutableStateOf(initialVolume) }
    var level by remember { mutableStateOf(initialLevel) }
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val done by viewModel.done.collectAsState()

    LaunchedEffect(done) { if (done) onBack() }

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
            Text(if (chapterId == null) "新建章节" else "编辑章节", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.save(novelId, chapterId, title, content, level, volume) }, enabled = !saving && title.isNotBlank()) {
                Icon(Icons.Outlined.Save, "保存", tint = AccentGold)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            // 卷名输入
            OutlinedTextField(
                value = volume,
                onValueChange = { volume = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("所属卷（可选）") },
                placeholder = { Text("如：第一卷 觉醒") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            // 级别选择
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("级别", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
                FilterChip(selected = level == 1, onClick = { level = 1 }, label = { Text("卷") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = level == 2, onClick = { level = 2 }, label = { Text("章") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = level == 3, onClick = { level = 3 }, label = { Text("节") })
            }
            Spacer(Modifier.height(12.dp))
            // 标题
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("章节标题*") },
                placeholder = { Text("如：第一章 启程") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            // 正文
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                label = { Text("章节正文") },
                placeholder = { Text("在这里输入章节内容...") },
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(24.dp))
            error?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            if (saving) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(28.dp)) }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
