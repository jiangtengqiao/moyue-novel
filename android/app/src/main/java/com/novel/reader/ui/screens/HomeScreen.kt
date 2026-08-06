package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.Category
import com.novel.reader.data.model.NovelBrief
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.components.*
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _featured = MutableStateFlow<List<NovelBrief>>(emptyList())
    val featured: StateFlow<List<NovelBrief>> = _featured

    private val _latest = MutableStateFlow<List<NovelBrief>>(emptyList())
    val latest: StateFlow<List<NovelBrief>> = _latest

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _categoryNovels = MutableStateFlow<List<NovelBrief>>(emptyList())
    val categoryNovels: StateFlow<List<NovelBrief>> = _categoryNovels

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val feat = repository.getFeatured()
                _featured.value = feat
                val cats = repository.getCategories()
                _categories.value = cats
                val resp = repository.getNovels(1, 20, sort = "latest")
                _latest.value = resp.items
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    fun selectCategory(name: String?) {
        _selectedCategory.value = name
        if (name != null) {
            viewModelScope.launch {
                try {
                    val resp = repository.getNovels(1, 20, category = name)
                    _categoryNovels.value = resp.items
                } catch (_: Exception) {}
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNovelClick: (String) -> Unit,
    onSearch: () -> Unit,
    onAnnouncements: () -> Unit,
) {
    val featured by viewModel.featured.collectAsState()
    val latest by viewModel.latest.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoryNovels by viewModel.categoryNovels.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "墨阅",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAnnouncements) {
                Icon(Icons.Outlined.NotificationsNone, "公告", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (loading) {
            LoadingIndicator(Modifier.height(400.dp))
        } else {
            // 精选推荐
            if (featured.isNotEmpty()) {
                SectionHeader("精选推荐")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(featured) { novel ->
                        NovelCard(novel = novel, onClick = { onNovelClick(novel.id) })
                    }
                }
            }

            // 分类筛选
            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryChip("全部", selectedCategory == null) { viewModel.selectCategory(null) }
                    }
                    items(categories) { cat ->
                        CategoryChip(cat.displayName, selectedCategory == cat.name) {
                            viewModel.selectCategory(cat.name)
                        }
                    }
                }
            }

            // 分类小说 or 最新小说
            Spacer(Modifier.height(8.dp))
            if (selectedCategory != null) {
                SectionHeader("分类作品")
                if (categoryNovels.isEmpty()) {
                    EmptyState("暂无作品", Modifier.height(200.dp))
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        categoryNovels.forEachIndexed { index, novel ->
                            NovelListItem(novel = novel, onClick = { onNovelClick(novel.id) })
                            if (index < categoryNovels.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            } else {
                SectionHeader("最新上架")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(latest) { novel ->
                        NovelCard(novel = novel, onClick = { onNovelClick(novel.id) })
                    }
                }
            }

            // 最新列表
            if (latest.isNotEmpty() && selectedCategory == null) {
                SectionHeader("热门榜单")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    latest.take(10).forEachIndexed { index, novel ->
                        RankingItem(rank = index + 1, novel = novel, onClick = { onNovelClick(novel.id) })
                        if (index < minOf(latest.size, 10) - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
fun RankingItem(rank: Int, novel: NovelBrief, onClick: () -> Unit) {
    val rankColor = when (rank) {
        1 -> AccentGold
        2 -> AccentGoldDark
        3 -> InkLight
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = rankColor,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.width(28.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = "${novel.author}  ${novel.wordCount / 10000}万字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
