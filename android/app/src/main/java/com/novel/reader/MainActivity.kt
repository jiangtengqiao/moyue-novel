package com.novel.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.novel.reader.ui.Routes
import com.novel.reader.ui.screens.*
import com.novel.reader.ui.theme.MoYueTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoYueTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoYueNavGraph()
                }
            }
        }
    }
}

@Composable
fun MoYueNavGraph(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH, exitTransition = { fadeOut(tween(300)) }) {
            SplashScreen(
                viewModel = mainViewModel,
                onNavigateToMain = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.LOGIN,
            enterTransition = { slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(300)) },
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 底部导航主容器
        composable(Routes.HOME) {
            MainScreen(navController = navController)
        }

        composable(
            Routes.NOVEL_DETAIL,
            arguments = listOf(navArgument("novelId") { type = NavType.StringType }),
            enterTransition = { slideInVertically(tween(400)) { it / 3 } + fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(300)) },
        ) { entry ->
            NovelDetailScreen(
                novelId = entry.arguments?.getString("novelId") ?: "",
                onBack = { navController.popBackStack() },
                onRead = { novelId, index ->
                    navController.navigate(Routes.reader(novelId, index))
                },
                onChapterList = { novelId ->
                    navController.navigate(Routes.chapterList(novelId))
                },
            )
        }

        composable(
            Routes.READER,
            arguments = listOf(
                navArgument("novelId") { type = NavType.StringType },
                navArgument("chapterIndex") { type = NavType.IntType },
            ),
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(300)) },
        ) { entry ->
            ReaderScreen(
                novelId = entry.arguments?.getString("novelId") ?: "",
                chapterIndex = entry.arguments?.getInt("chapterIndex") ?: 0,
                onBack = { navController.popBackStack() },
                onChapterChange = { index ->
                    navController.navigate(Routes.reader(entry.arguments?.getString("novelId") ?: "", index)) {
                        popUpTo(Routes.READER) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNovelClick = { id -> navController.navigate(Routes.novelDetail(id)) }
            )
        }

        composable(Routes.ANNOUNCEMENTS) {
            AnnouncementsScreen(
                onBack = { navController.popBackStack() },
                onAnnouncementClick = { id -> navController.navigate(Routes.announcementDetail(id)) }
            )
        }

        composable(
            Routes.ANNOUNCEMENT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            AnnouncementDetailScreen(
                announcementId = entry.arguments?.getString("id") ?: "",
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CREATOR_REGISTER) {
            CreatorRegisterScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATOR_DASHBOARD) {
            CreatorDashboardScreen(
                onCreateNovel = { navController.navigate(Routes.CREATE_NOVEL) },
                onMyNovels = { navController.navigate(Routes.MY_NOVELS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_NOVEL) {
            CreateNovelScreen(
                onCreated = { novelId ->
                    navController.navigate(Routes.uploadNovel(novelId)) {
                        popUpTo(Routes.CREATE_NOVEL) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.UPLOAD_NOVEL,
            arguments = listOf(navArgument("novelId") { type = NavType.StringType }),
        ) { entry ->
            UploadNovelScreen(
                novelId = entry.arguments?.getString("novelId") ?: "",
                onUploadStarted = { taskId ->
                    navController.navigate(Routes.uploadProgress(taskId)) {
                        popUpTo(Routes.UPLOAD_NOVEL) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.UPLOAD_PROGRESS,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { entry ->
            UploadProgressScreen(
                taskId = entry.arguments?.getString("taskId") ?: "",
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MY_NOVELS) {
            MyNovelsScreen(
                onNovelClick = { id -> navController.navigate(Routes.novelDetail(id)) },
                onUpload = { id -> navController.navigate(Routes.uploadNovel(id)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
