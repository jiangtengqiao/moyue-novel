package com.novel.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.novel.reader.ui.Routes
import com.novel.reader.ui.theme.*

data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
fun MainScreen(navController: NavController) {
    val tabs = remember {
        listOf(
            TabItem("home_tab", "书城", Icons.Outlined.Home, Icons.Filled.Home),
            TabItem("library_tab", "书架", Icons.Outlined.CollectionsBookmark, Icons.Filled.CollectionsBookmark),
            TabItem("creator_tab", "创作", Icons.Outlined.EditNote, Icons.Filled.EditNote),
            TabItem("profile_tab", "我的", Icons.Outlined.Person, Icons.Filled.Person),
        )
    }

    val tabNavController = rememberNavController()
    var selectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomBar(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    selectedIndex = index
                    tabNavController.navigate(tabs[index].route) {
                        popUpTo(tabNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = tabNavController,
                startDestination = "home_tab",
            ) {
                composable("home_tab") { HomeScreen(onNovelClick = { id -> navController.navigate(Routes.novelDetail(id)) }, onSearch = { navController.navigate(Routes.SEARCH) }, onAnnouncements = { navController.navigate(Routes.ANNOUNCEMENTS) }) }
                composable("library_tab") { LibraryScreen(onNovelClick = { id -> navController.navigate(Routes.novelDetail(id)) }) }
                composable("creator_tab") { CreatorScreen(navController = navController) }
                composable("profile_tab") { ProfileScreen(navController = navController) }
            }
        }
    }
}

@Composable
private fun BottomBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(300),
                    label = "tabColor"
                )
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.9f,
                    animationSpec = tween(300),
                    label = "tabScale"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                            .graphicsLayerScale(scale),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.graphicsLayerScale(scale: Float): Modifier {
    return this.then(
        Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}
