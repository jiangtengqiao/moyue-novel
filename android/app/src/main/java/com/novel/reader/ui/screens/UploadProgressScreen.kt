package com.novel.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.novel.reader.ui.theme.*

@Composable
fun UploadProgressScreen(
    taskId: String,
    viewModel: UploadViewModel = hiltViewModel(),
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(taskId) { viewModel.pollTask(taskId) }

    val task by viewModel.progressTask.collectAsState()

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
            Text("处理进度", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }

        if (task == null) {
            com.novel.reader.ui.components.LoadingIndicator()
            return@Column
        }

        val t = task!!
        val isComplete = t.status == "completed"
        val isFailed = t.status == "failed"

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // 进度环
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = (t.progress / 100f).toFloat(),
                        animationSpec = tween(500, easing = FastOutSlowInEasing),
                        label = "progress"
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        color = if (isFailed) StatusError else AccentGold,
                        strokeWidth = 6.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${t.progress.toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                        )
                        if (isComplete) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 状态信息
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (t.status) {
                            "pending" -> "等待处理"
                            "processing" -> "处理中"
                            "completed" -> "处理完成"
                            "failed" -> "处理失败"
                            else -> t.status
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isComplete -> StatusSuccess
                            isFailed -> StatusError
                            else -> AccentGold
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(t.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (t.currentFile != null && t.status == "processing") {
                        Spacer(Modifier.height(8.dp))
                        Text("当前文件: ${t.currentFile}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard("总文件", t.totalFiles.toString())
                        StatCard("已处理", t.processedFiles.toString())
                        StatCard("失败", t.failedFiles.toString())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 文件列表
            if (t.fileList.isNotEmpty()) {
                Text("文件明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(t.fileList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val statusIcon = when (item.status) {
                                "completed" -> Icons.Outlined.CheckCircle to StatusSuccess
                                "failed" -> Icons.Outlined.Cancel to StatusError
                                "processing" -> Icons.Outlined.HourglassEmpty to AccentGold
                                else -> Icons.Outlined.Description to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Icon(statusIcon.first, contentDescription = null, tint = statusIcon.second, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.filename,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            if (item.chapters > 0) {
                                Text("${item.chapters}章", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                            }
                        }
                    }
                }
            }

            // 完成按钮
            if (isComplete || isFailed) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = PaperWhite),
                ) {
                    Text(if (isComplete) "完成" else "返回首页")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
