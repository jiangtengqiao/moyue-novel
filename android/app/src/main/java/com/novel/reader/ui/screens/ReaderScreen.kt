package com.novel.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.Chapter
import com.novel.reader.data.model.ChapterContent
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.data.repository.SessionManager
import com.novel.reader.ui.components.LoadingIndicator
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: NovelRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _chapter = MutableStateFlow<ChapterContent?>(null)
    val chapter: StateFlow<ChapterContent?> = _chapter

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings

    private val _fontSize = MutableStateFlow(18)
    val fontSize: StateFlow<Int> = _fontSize

    private val _nightMode = MutableStateFlow(false)
    val nightMode: StateFlow<Boolean> = _nightMode

    private val _bgIndex = MutableStateFlow(0)
    val bgIndex: StateFlow<Int> = _bgIndex

    private val _chapterCount = MutableStateFlow(0)
    val chapterCount: StateFlow<Int> = _chapterCount

    init {
        viewModelScope.launch {
            _fontSize.value = sessionManager.fontSizeFlow.first()
            _nightMode.value = sessionManager.nightModeFlow.first()
            _bgIndex.value = sessionManager.readerBgFlow.first()
        }
    }

    fun load(novelId: String, index: Int) {
        viewModelScope.launch {
            _loading.value = true
            _showMenu.value = false
            try {
                if (_chapterCount.value == 0) {
                    try { _chapterCount.value = repository.getChapters(novelId).size } catch (_: Exception) {}
                }
                val ch = repository.getChapterContent(novelId, index)
                _chapter.value = ch
                try { repository.saveReadingProgress(novelId, index, ch.title) } catch (_: Exception) {}
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    fun toggleMenu() { _showMenu.value = !_showMenu.value }
    fun toggleSettings() { _showSettings.value = !_showSettings.value }

    fun changeFontSize(delta: Int) {
        val newSize = (_fontSize.value + delta).coerceIn(14, 32)
        _fontSize.value = newSize
        viewModelScope.launch { sessionManager.saveReaderPrefs(newSize, _nightMode.value, _bgIndex.value) }
    }

    fun toggleNightMode() {
        _nightMode.value = !_nightMode.value
        _bgIndex.value = 3 // 夜间模式对应 ReaderBgDark
        viewModelScope.launch { sessionManager.saveReaderPrefs(_fontSize.value, _nightMode.value, _bgIndex.value) }
    }

    fun setBgIndex(index: Int) {
        _bgIndex.value = index
        _nightMode.value = index == 3
        viewModelScope.launch { sessionManager.saveReaderPrefs(_fontSize.value, _nightMode.value, index) }
    }
}

private val readerBgs = listOf(
    Triple(ReaderBgDefault, Color(0xFF1A1A1A), "默认"),
    Triple(ReaderBgSepia, Color(0xFF5B4636), "护眼"),
    Triple(ReaderBgGreen, Color(0xFF2D4A2D), "绿色"),
    Triple(ReaderBgDark, Color(0xFFE0E0E0), "夜间"),
)

@Composable
fun ReaderScreen(
    novelId: String,
    chapterIndex: Int,
    viewModel: ReaderViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChapterChange: (Int) -> Unit,
    onChapterList: (String) -> Unit,
) {
    LaunchedEffect(novelId, chapterIndex) { viewModel.load(novelId, chapterIndex) }

    val chapter by viewModel.chapter.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val showMenu by viewModel.showMenu.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val nightMode by viewModel.nightMode.collectAsState()
    val bgIndex by viewModel.bgIndex.collectAsState()
    val chapterCount by viewModel.chapterCount.collectAsState()

    val (bgColor, textColor) = readerBgs[bgIndex.coerceIn(0, 3)]
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100 && chapterIndex > 0) {
                            onChapterChange(chapterIndex - 1)
                        } else if (dragOffset < -100 && chapterIndex < chapterCount - 1) {
                            onChapterChange(chapterIndex + 1)
                        }
                        dragOffset = 0f
                    }
                ) { _, dragAmount ->
                    dragOffset += dragAmount
                }
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) { viewModel.toggleMenu() }
    ) {
        if (loading) {
            LoadingIndicator()
        } else {
            val ch = chapter
            if (ch != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                ) {
                    Text(
                        text = ch.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.2f))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = ch.content,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.8).sp,
                        color = textColor,
                        fontFamily = FontFamily.Serif,
                    )
                    Spacer(Modifier.height(60.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (chapterIndex > 0) {
                            TextButton(onClick = { onChapterChange(chapterIndex - 1) }) {
                                Icon(Icons.AutoMirrored.Outlined.NavigateBefore, contentDescription = null, tint = textColor)
                                Text("上一章", color = textColor)
                            }
                        } else { Spacer(Modifier.width(1.dp)) }
                        if (chapterIndex < chapterCount - 1) {
                            TextButton(onClick = { onChapterChange(chapterIndex + 1) }) {
                                Text("下一章", color = textColor)
                                Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null, tint = textColor)
                            }
                        } else {
                            Text("已读完", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        // 顶部菜单
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = chapter?.title ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onChapterList(novelId) }) {
                        Icon(Icons.Outlined.List, "目录", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(Icons.Outlined.Settings, "设置", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // 底部设置面板
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    // 字体大小
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("字号", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
                        IconButton(onClick = { viewModel.changeFontSize(-2) }) {
                            Text("A-", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            "$fontSize",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(onClick = { viewModel.changeFontSize(2) }) {
                            Text("A+", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 背景色选择
                    Text("背景", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        readerBgs.forEachIndexed { index, (color, _, label) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                                    .clickable { viewModel.setBgIndex(index) }
                                    .then(
                                        if (bgIndex == index) Modifier.border(2.dp, AccentGold, androidx.compose.foundation.shape.CircleShape)
                                        else Modifier
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 夜间模式
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleNightMode() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(12.dp))
                        Text("夜间模式", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = nightMode, onCheckedChange = { viewModel.toggleNightMode() })
                    }
                }
            }
        }
    }
}
