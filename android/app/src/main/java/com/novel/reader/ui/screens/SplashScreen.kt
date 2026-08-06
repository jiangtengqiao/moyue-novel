package com.novel.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.reader.MainViewModel
import com.novel.reader.SplashState
import com.novel.reader.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val splashState by viewModel.splashState.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val downloading by viewModel.downloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var logoVisible by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAnnouncements by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        logoVisible = true
    }

    // 处理启动状态
    LaunchedEffect(splashState) {
        when (splashState) {
            SplashState.GoToMain -> {
                delay(800)
                val info = updateInfo
                if (info?.response?.hasUpdate == true && info.response.latestVersion != null) {
                    showUpdateDialog = true
                } else if (info != null && info.announcements.isNotEmpty()) {
                    showAnnouncements = true
                } else {
                    onNavigateToMain()
                }
            }
            SplashState.GoToLogin -> {
                delay(800)
                onNavigateToLogin()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo 动画
            val scale by animateFloatAsState(
                targetValue = if (logoVisible) 1f else 0.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "logoScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (logoVisible) 1f else 0f,
                animationSpec = tween(800, easing = FastOutSlowInEasing),
                label = "logoAlpha"
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(listOf(InkBlack, InkDark))
                    )
                    .graphicsLayerAlpha(alpha, scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "墨",
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                    color = PaperWhite,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "墨阅",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.alpha(alpha),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "原创文学阅读平台",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha),
            )
            Spacer(Modifier.height(40.dp))
            if (splashState is SplashState.Loading) {
                CircularProgressIndicator(
                    color = AccentGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // 更新弹窗
    if (showUpdateDialog && updateInfo?.response?.latestVersion != null) {
        val version = updateInfo!!.response.latestVersion!!
        AlertDialog(
            onDismissRequest = {
                if (!version.forceUpdate) {
                    showUpdateDialog = false
                    val ann = updateInfo?.announcements
                    if (!ann.isNullOrEmpty()) showAnnouncements = true
                    else onNavigateToMain()
                }
            },
            title = {
                Text(version.updateTitle ?: "发现新版本", fontFamily = FontFamily.Serif)
            },
            text = {
                Column {
                    Text(
                        text = "v${version.versionName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentGold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = version.updateLog,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (downloading) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = AccentGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "下载中... ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.downloadAndInstall() },
                    enabled = !downloading,
                ) {
                    Text(if (downloading) "下载中" else "立即更新", color = AccentGold)
                }
            },
            dismissButton = {
                if (!version.forceUpdate) {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        val ann = updateInfo?.announcements
                        if (!ann.isNullOrEmpty()) showAnnouncements = true
                        else onNavigateToMain()
                    }) {
                        Text("稍后再说", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
    }

    // 公告弹窗
    if (showAnnouncements && !updateInfo?.announcements.isNullOrEmpty()) {
        val anns = updateInfo!!.announcements
        var currentIndex by remember { mutableStateOf(0) }
        val ann = anns[currentIndex]

        AlertDialog(
            onDismissRequest = {
                if (currentIndex < anns.size - 1) {
                    currentIndex++
                } else {
                    showAnnouncements = false
                    onNavigateToMain()
                }
            },
            title = { Text(ann.title, fontFamily = FontFamily.Serif) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = ann.content,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (currentIndex < anns.size - 1) {
                        currentIndex++
                    } else {
                        showAnnouncements = false
                        onNavigateToMain()
                    }
                }) {
                    Text(
                        if (currentIndex < anns.size - 1) "下一条" else "我知道了",
                        color = AccentGold
                    )
                }
            },
        )
    }
}

@Composable
private fun Modifier.graphicsLayerAlpha(alpha: Float, scale: Float): Modifier {
    return this.then(
        Modifier.androidxGraphicsLayer(alpha, scale)
    )
}

private fun Modifier.androidxGraphicsLayer(alpha: Float, scale: Float): Modifier =
    this.then(
        androidx.compose.ui.graphics.graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            alpha = alpha,
        )
    )
