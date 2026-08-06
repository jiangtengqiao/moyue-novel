package com.novel.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.ChapterContent
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _chapter = MutableStateFlow<ChapterContent?>(null)
    val chapter: StateFlow<ChapterContent?> = _chapter

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu

    private var fontSize = 18
    private val _fontSizeFlow = MutableStateFlow(18)
    val fontSizeFlow: StateFlow<Int> = _fontSizeFlow

    fun load(novelId: String, index: Int) {
        viewModelScope.launch {
            _loading.value = true
            _showMenu.value = false
            try {
                _chapter.value = repository.getChapterContent(novelId, index)
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    fun toggleMenu() { _showMenu.value = !_showMenu.value }

    fun changeFontSize(delta: Int) {
        fontSize = (fontSize + delta).coerceIn(14, 28)
        _fontSizeFlow.value = fontSize
    }
}

@Composable
fun ReaderScreen(
    novelId: String,
    chapterIndex: Int,
    viewModel: ReaderViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChapterChange: (Int) -> Unit,
) {
    LaunchedEffect(novelId, chapterIndex) { viewModel.load(novelId, chapterIndex) }

    val chapter by viewModel.chapter.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val showMenu by viewModel.showMenu.collectAsState()
    val fontSize by viewModel.fontSizeFlow.collectAsState()

    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100) {
                            // 右滑 - 上一章
                            if (chapterIndex > 0) onChapterChange(chapterIndex - 1)
                        } else if (dragOffset < -100) {
                            // 左滑 - 下一章
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
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = ch.content,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.8).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Serif,
                    )
                    Spacer(Modifier.height(60.dp))

                    // 翻页按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (chapterIndex > 0) {
                            TextButton(onClick = { onChapterChange(chapterIndex - 1) }) {
                                Icon(Icons.AutoMirrored.Outlined.NavigateBefore, contentDescription = null)
                                Text("上一章")
                            }
                        } else { Spacer(Modifier.width(1.dp)) }
                        TextButton(onClick = { onChapterChange(chapterIndex + 1) }) {
                            Text("下一章")
                            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
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
                    IconButton(onClick = { viewModel.changeFontSize(-2) }) {
                        Text("A-", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { viewModel.changeFontSize(2) }) {
                        Text("A+", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
