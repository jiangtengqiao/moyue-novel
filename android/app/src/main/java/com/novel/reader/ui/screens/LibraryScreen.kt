package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.NovelBrief
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.data.repository.SessionManager
import com.novel.reader.ui.components.*
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: NovelRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _bookmarks = MutableStateFlow<List<NovelBrief>>(emptyList())
    val bookmarks: StateFlow<List<NovelBrief>> = _bookmarks

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init { loadBookmarks() }

    fun loadBookmarks() {
        viewModelScope.launch {
            val token = sessionManager.tokenFlow.first()
            if (token == null) {
                _isLoggedIn.value = false
                _loading.value = false
                return
            }
            _isLoggedIn.value = true
            _loading.value = true
            try {
                _bookmarks.value = repository.getBookmarks()
            } catch (_: Exception) {}
            _loading.value = false
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNovelClick: (String) -> Unit,
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Text(
            text = "书架",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        if (!isLoggedIn) {
            EmptyState("登录后查看你的书架", Modifier.fillMaxSize())
        } else if (loading) {
            LoadingIndicator()
        } else if (bookmarks.isEmpty()) {
            EmptyState("书架空空如也\n去书城发现好书吧", Modifier.fillMaxSize())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(bookmarks) { novel ->
                    NovelListItem(
                        novel = novel,
                        onClick = { onNovelClick(novel.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
