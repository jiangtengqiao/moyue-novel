package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
class CreateNovelViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createdId = MutableStateFlow<String?>(null)
    val createdId: StateFlow<String?> = _createdId

    fun create(title: String, author: String, desc: String, tags: List<String>) {
        if (title.isBlank() || author.isBlank()) {
            _error.value = "请填写书名和作者"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val novel = repository.createNovel(title, author, desc, tags)
                _createdId.value = novel.id
            } catch (e: Exception) {
                _error.value = "创建失败: ${e.message}"
            }
            _loading.value = false
        }
    }
}

@Composable
fun CreateNovelScreen(
    viewModel: CreateNovelViewModel = hiltViewModel(),
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val createdId by viewModel.createdId.collectAsState()

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }

    LaunchedEffect(createdId) { createdId?.let { onCreated(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("创建新作品", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(8.dp))
        Text("填写作品基本信息，创建后即可上传章节内容", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("书名 *") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = author, onValueChange = { author = it },
            label = { Text("作者 *") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = desc, onValueChange = { desc = it },
            label = { Text("简介") },
            modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = tagsText, onValueChange = { tagsText = it },
            label = { Text("标签(用逗号分隔)") }, singleLine = true,
            placeholder = { Text("如: 玄幻, 冒险, 成长") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val tags = tagsText.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
                viewModel.create(title, author, desc, tags)
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = PaperWhite),
        ) {
            if (loading) {
                CircularProgressIndicator(color = PaperWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("创建作品", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
