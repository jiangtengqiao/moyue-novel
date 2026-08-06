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
}

@Composable
fun MyNovelsScreen(
    viewModel: MyNovelsViewModel = hiltViewModel(),
    onNovelClick: (String) -> Unit,
    onUpload: (String) -> Unit,
    onBack: () -> Unit,
) {
    val novels by viewModel.novels.collectAsState()
    val loading by viewModel.loading.collectAsState()

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
            Text("我的作品", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }

        if (loading) {
            LoadingIndicator()
        } else if (novels.isEmpty()) {
            EmptyState("还没有作品\n去创建你的第一部作品吧", Modifier.fillMaxSize())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(novels) { novel ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp)),
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
                                IconButton(onClick = { onUpload(novel.id) }) {
                                    Icon(Icons.Outlined.Upload, contentDescription = "上传", tint = AccentGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
