package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.novel.reader.ui.theme.*
import java.io.InputStream

@Composable
fun AgreementScreen(
    title: String,
    fileName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("加载中...") }

    LaunchedEffect(fileName) {
        content = try {
            val stream: InputStream = context.assets.open("agreements/$fileName")
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) { "无法加载协议文件: ${e.message}" }
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }
        HorizontalDivider()
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            lineHeight = androidx.compose.ui.unit.TextUnit(1.8f, androidx.compose.ui.unit.TextUnitType.Em),
        )
    }
}
