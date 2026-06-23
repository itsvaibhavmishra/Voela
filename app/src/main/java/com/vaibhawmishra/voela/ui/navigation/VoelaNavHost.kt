package com.vaibhawmishra.voela.ui.navigation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaibhawmishra.voela.ui.components.PlaceholderScreen
import com.vaibhawmishra.voela.ui.home.HomeScreen
import com.vaibhawmishra.voela.ui.youtube.YouTubeUrlScreen
import com.vaibhawmishra.voela.ui.youtube.YouTubeViewModel

private object Routes {
    const val HOME = "home"
    const val YOUTUBE = "youtube"
    const val LIBRARY = "library"
    const val FEATURE = "feature/{name}"
    const val SPLIT = "split/{name}"
    fun feature(name: String) = "feature/${Uri.encode(name)}"
    fun split(name: String) = "split/${Uri.encode(name)}"
}

@Composable
fun VoelaNavHost() {
    val navController = rememberNavController()
    val duration = 300
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(duration)) + fadeIn(tween(duration)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Left, tween(duration)) + fadeOut(tween(duration)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.Right, tween(duration)) + fadeIn(tween(duration)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(duration)) + fadeOut(tween(duration)) },
    ) {
        composable(Routes.HOME) {
            val context = LocalContext.current
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { navController.navigate(Routes.feature(displayName(context, it))) }
            }
            HomeScreen(
                recents = emptyList(),
                onChooseFile = { picker.launch("audio/*") },
                onYouTubeUrl = { navController.navigate(Routes.YOUTUBE) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onRecentClick = { navController.navigate(Routes.split(it.title)) },
            )
        }
        composable(Routes.YOUTUBE) {
            val viewModel: YouTubeViewModel = viewModel(factory = YouTubeViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            YouTubeUrlScreen(
                uiState = state,
                onBack = navController::popBackStack,
                onUrlChange = viewModel::onUrlChange,
                onExtract = viewModel::onExtract,
                onContinue = { navController.navigate(Routes.feature("Extracted Audio")) },
                onClearResult = viewModel::onClearResult,
                onPlayPause = viewModel::onPlayPause,
                onSeek = viewModel::onSeek,
                onDownload = viewModel::onDownload,
                onMessageShown = viewModel::onMessageShown,
                onClearRecents = viewModel::onClearRecents,
                onOpenLink = {},
            )
        }
        composable(Routes.LIBRARY) {
            PlaceholderScreen(title = "Library", onBack = navController::popBackStack)
        }
        composable(Routes.FEATURE, arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
            PlaceholderScreen(
                title = "Select Feature Screen",
                subtitle = entry.arguments?.getString("name").orEmpty(),
                onBack = navController::popBackStack,
            )
        }
        composable(Routes.SPLIT, arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
            PlaceholderScreen(
                title = "Splitting Screen",
                subtitle = entry.arguments?.getString("name").orEmpty(),
                onBack = navController::popBackStack,
            )
        }
    }
}

// Resolve a picked file's display name for the Select Feature screen
private fun displayName(context: Context, uri: Uri): String {
    val name = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
    return name ?: "Audio"
}
