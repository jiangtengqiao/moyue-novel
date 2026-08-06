package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
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
import androidx.navigation.NavController
import com.novel.reader.data.model.User
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.data.repository.SessionManager
import com.novel.reader.ui.Routes
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: NovelRepository,
) : ViewModel() {
    val userFlow = sessionManager.userFlow

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        viewModelScope.launch {
            _isLoggedIn.value = sessionManager.tokenFlow.first() != null
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
) {
    val user by viewModel.userFlow.collectAsState(initial = null)
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        )

        // 用户信息卡片
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable {
                    if (!isLoggedIn) navController.navigate(Routes.LOGIN)
                }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(InkBlack, InkDark))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (user?.nickname ?: user?.username ?: "客").take(1),
                        style = MaterialTheme.typography.headlineSmall,
                        color = PaperWhite,
                        fontFamily = FontFamily.Serif,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = user?.nickname ?: user?.username ?: "点击登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (user != null) {
                        Text(
                            text = "@${user!!.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 菜单项
        val menuItems = if (isLoggedIn) {
            listOf(
                MenuItemData("创作者中心", Icons.Outlined.Dashboard, Routes.CREATOR_DASHBOARD),
                MenuItemData("我的作品", Icons.Outlined.MenuBook, Routes.MY_NOVELS),
                MenuItemData("阅读历史", Icons.Outlined.History, null),
                MenuItemData("公告中心", Icons.Outlined.Campaign, Routes.ANNOUNCEMENTS),
                MenuItemData("设置", Icons.Outlined.Settings, null),
            )
        } else {
            listOf(
                MenuItemData("公告中心", Icons.Outlined.Campaign, Routes.ANNOUNCEMENTS),
                MenuItemData("设置", Icons.Outlined.Settings, null),
            )
        }

        menuItems.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        item.route?.let { navController.navigate(it) }
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(item.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            if (item != menuItems.last()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        if (isLoggedIn) {
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text("退出登录", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

data class MenuItemData(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String?,
)
