package com.novel.reader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.UploadTask
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _taskId = MutableStateFlow<String?>(null)
    val taskId: StateFlow<String?> = _taskId

    private val _progressTask = MutableStateFlow<UploadTask?>(null)
    val progressTask: StateFlow<UploadTask?> = _progressTask

    fun uploadSingle(novelId: String, file: File) {
        viewModelScope.launch {
            _uploading.value = true
            _error.value = null
            try {
                val result = repository.uploadSingleFile(novelId, file)
                _error.value = "上传成功: ${result.message}"
            } catch (e: Exception) {
                _error.value = "上传失败: ${e.message}"
            }
            _uploading.value = false
        }
    }

    fun uploadFolder(novelId: String, files: List<File>) {
        viewModelScope.launch {
            _uploading.value = true
            _error.value = null
            try {
                val task = repository.uploadFolder(novelId, files)
                _taskId.value = task.id
            } catch (e: Exception) {
                _error.value = "上传失败: ${e.message}"
            }
            _uploading.value = false
        }
    }

    fun pollTask(taskId: String) {
        viewModelScope.launch {
            while (true) {
                try {
                    val task = repository.getUploadTask(taskId)
                    _progressTask.value = task
                    if (task.status == "completed" || task.status == "failed") break
                } catch (_: Exception) { break }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}

@Composable
fun UploadNovelScreen(
    novelId: String,
    viewModel: UploadViewModel = hiltViewModel(),
    onUploadStarted: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uploading by viewModel.uploading.collectAsState()
    val error by viewModel.error.collectAsState()
    val taskId by viewModel.taskId.collectAsState()

    val selectedFiles = remember { mutableStateListOf<File>() }

    // 单文件选择
    val singlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            if (file != null) {
                viewModel.uploadSingle(novelId, file)
            }
        }
    }

    // 多文件选择 (文件夹)
    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uriToFile(context, it) }
            if (files.isNotEmpty()) {
                viewModel.uploadFolder(novelId, files)
            }
        }
    }

    LaunchedEffect(taskId) {
        taskId?.let {
            onUploadStarted(it)
        }
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
            Text("上传内容", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // 上传说明
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("支持的文件格式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("TXT  -  纯文本格式，自动识别编码", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("MD   -  Markdown 格式，自动转换", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("DOCX -  Word 文档，自动解析段落", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("系统会自动识别章节标题并切分章节", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 上传按钮
            UploadOptionCard(
                title = "上传单个文件",
                desc = "选择一个 TXT / MD / DOCX 文件",
                icon = Icons.Outlined.UploadFile,
            ) {
                singlePicker.launch(arrayOf("text/plain", "text/markdown", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*"))
            }

            Spacer(Modifier.height(12.dp))

            UploadOptionCard(
                title = "批量上传文件夹",
                desc = "选择多个文件，自动解析为章节",
                icon = Icons.Outlined.Folder,
            ) {
                multiPicker.launch(arrayOf("text/plain", "text/markdown", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*"))
            }

            Spacer(Modifier.height(16.dp))
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = if (it.startsWith("上传成功")) StatusSuccess else MaterialTheme.colorScheme.error)
            }

            if (uploading) {
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentGold, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("正在上传...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadOptionCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

/** 将 Uri 转为临时 File */
fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val fileName = if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else "upload_${System.currentTimeMillis()}.txt"
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            tempFile
        }
    } catch (e: Exception) { null }
}
