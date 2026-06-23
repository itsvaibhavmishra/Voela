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
import com.vaibhawmishra.voela.ui.feature.SelectFeatureScreen
import com.vaibhawmishra.voela.ui.feature.SelectFeatureViewModel
import com.vaibhawmishra.voela.ui.home.HomeScreen
import com.vaibhawmishra.voela.ui.trim.TrimAudioScreen
import com.vaibhawmishra.voela.ui.trim.TrimAudioViewModel
import com.vaibhawmishra.voela.ui.trim.TrimFeature
import com.vaibhawmishra.voela.ui.youtube.YouTubeUrlScreen
import com.vaibhawmishra.voela.ui.youtube.YouTubeViewModel

private object Routes {
    const val HOME = "home"
    const val YOUTUBE = "youtube"
    const val LIBRARY = "library"
    const val FEATURE = "feature/{name}/{source}"
    const val TRIM = "trim/{feature}/{name}/{source}"
    const val PROCESS = "process/{feature}/{name}/{source}/{start}/{end}"
    const val SPLIT = "split/{name}"
    fun feature(name: String, source: String) = "feature/${Uri.encode(name)}/${Uri.encode(source)}"
    fun trim(feature: String, name: String, source: String) = "trim/$feature/${Uri.encode(name)}/${Uri.encode(source)}"
    fun process(feature: String, name: String, source: String, start: Long, end: Long) =
        "process/$feature/${Uri.encode(name)}/${Uri.encode(source)}/$start/$end"
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
                uri?.let { navController.navigate(Routes.feature(displayName(context, it), it.toString())) }
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
                onContinue = { state.result?.let { navController.navigate(Routes.feature(it.title, it.localPath)) } },
                onClearResult = viewModel::onClearResult,
                onPlayPause = viewModel::onPlayPause,
                onSeek = viewModel::onSeek,
                onDownload = viewModel::onDownload,
                onMessageShown = viewModel::onMessageShown,
                onClearRecents = viewModel::onClearRecents,
                onOpenLink = viewModel::onOpenLink,
            )
        }
        composable(Routes.LIBRARY) {
            PlaceholderScreen(title = "Library", onBack = navController::popBackStack)
        }
        composable(
            Routes.FEATURE,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType },
            ),
        ) { entry ->
            val name = entry.arguments?.getString("name").orEmpty()
            val source = entry.arguments?.getString("source").orEmpty()
            val viewModel: SelectFeatureViewModel = viewModel(factory = SelectFeatureViewModel.factory(name, source))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SelectFeatureScreen(
                uiState = state,
                onBack = navController::popBackStack,
                onPlayPause = viewModel::onPlayPause,
                onSplitVocals = { navController.navigate(Routes.trim(TrimFeature.VOCALS.key, name, source)) },
                onSplitAudio = { navController.navigate(Routes.trim(TrimFeature.AUDIO.key, name, source)) },
            )
        }
        composable(
            Routes.TRIM,
            arguments = listOf(
                navArgument("feature") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType },
            ),
        ) { entry ->
            val feature = entry.arguments?.getString("feature")
            val name = entry.arguments?.getString("name").orEmpty()
            val source = entry.arguments?.getString("source").orEmpty()
            val viewModel: TrimAudioViewModel =
                viewModel(factory = TrimAudioViewModel.factory(TrimFeature.from(feature), name, source))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            TrimAudioScreen(
                uiState = state,
                onBack = navController::popBackStack,
                onPlayPause = viewModel::onPlayPause,
                onRangeChange = viewModel::onRangeChange,
                onStartStep = viewModel::onStartStep,
                onEndStep = viewModel::onEndStep,
                onProceed = {
                    navController.navigate(Routes.process(state.feature.key, name, source, state.startMs, state.endMs))
                },
            )
        }
        composable(
            Routes.PROCESS,
            arguments = listOf(
                navArgument("feature") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType },
                navArgument("start") { type = NavType.LongType },
                navArgument("end") { type = NavType.LongType },
            ),
        ) { entry ->
            val feature = TrimFeature.from(entry.arguments?.getString("feature"))
            val start = entry.arguments?.getLong("start") ?: 0L
            val end = entry.arguments?.getLong("end") ?: 0L
            PlaceholderScreen(
                title = if (feature == TrimFeature.VOCALS) "Split Vocals" else "Split Audio",
                subtitle = "${TrimAudioViewModel.formatPrecise(start)} – ${TrimAudioViewModel.formatPrecise(end)}",
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
