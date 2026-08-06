package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.novel.reader.data.model.Announcement
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.components.LoadingIndicator
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _list = MutableStateFlow<List<Announcement>>(emptyList())
    val list: StateFlow<List<Announcement>> = _list

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try { _list.value = repository.getAnnouncements() } catch (_: Exception) {}
            _loading.value = false
        }
    }
}

@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
) {
    val list by viewModel.list.collectAsState()
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
            Text("公告中心", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        }

        if (loading) {
            LoadingIndicator()
        } else if (list.isEmpty()) {
            com.novel.reader.ui.components.EmptyState("暂无公告")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list) { ann ->
                    AnnouncementCard(ann = ann, onClick = { onAnnouncementClick(ann.id) })
                }
            }
        }
    }
}

@Composable
fun AnnouncementCard(ann: Announcement, onClick: () -> Unit) {
    val typeColor = when (ann.type) {
        "warning" -> StatusWarning
        "update" -> AccentGold
        "activity" -> StatusInfo
        else -> InkLight
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ann.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (ann.isPinned) {
                    Text("置顶", style = MaterialTheme.typography.labelSmall, color = typeColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = ann.content.take(60) + if (ann.content.length > 60) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun AnnouncementDetailScreen(
    announcementId: String,
    viewModel: AnnouncementsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val list by viewModel.list.collectAsState()
    val ann = remember(list, announcementId) { list.find { it.id == announcementId } }

    LaunchedEffect(Unit) { if (list.isEmpty()) viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
        }
        if (ann != null) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = ann.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ann.createdAt?.take(10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = ann.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp),
                )
            }
        }
    }
}
