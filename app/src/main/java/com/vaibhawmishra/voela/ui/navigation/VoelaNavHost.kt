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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaibhawmishra.voela.ui.feature.SelectFeatureScreen
import com.vaibhawmishra.voela.ui.feature.SelectFeatureViewModel
import com.vaibhawmishra.voela.ui.home.HomeScreen
import com.vaibhawmishra.voela.ui.home.HomeViewModel
import com.vaibhawmishra.voela.ui.library.LibraryScreen
import com.vaibhawmishra.voela.ui.library.LibraryViewModel
import com.vaibhawmishra.voela.ui.result.ResultScreen
import com.vaibhawmishra.voela.ui.result.ResultViewModel
import com.vaibhawmishra.voela.ui.split.SplitScreen
import com.vaibhawmishra.voela.ui.split.SplitViewModel
import com.vaibhawmishra.voela.ui.trim.TrimAudioScreen
import com.vaibhawmishra.voela.ui.trim.TrimAudioViewModel
import com.vaibhawmishra.voela.ui.trim.TrimFeature
import com.vaibhawmishra.voela.ui.youtube.YouTubeUrlScreen
import com.vaibhawmishra.voela.ui.youtube.YouTubeViewModel

private object Routes {
    const val HOME = "home"
    const val YOUTUBE = "youtube?lib={lib}"
    const val LIBRARY = "library"
    const val FEATURE = "feature/{name}/{source}"
    const val TRIM = "trim/{feature}/{name}/{source}"
    const val PROCESS = "process/{feature}/{name}/{source}/{start}/{end}/{engine}"
    const val RESULT = "result/{feature}/{name}/{elapsed}?lib={lib}"
    fun youtube(lib: String = "") = "youtube?lib=${Uri.encode(lib)}"
    fun feature(name: String, source: String) = "feature/${Uri.encode(name)}/${Uri.encode(source)}"
    fun trim(feature: String, name: String, source: String) = "trim/$feature/${Uri.encode(name)}/${Uri.encode(source)}"
    fun process(feature: String, name: String, source: String, start: Long, end: Long, engine: String) =
        "process/$feature/${Uri.encode(name)}/${Uri.encode(source)}/$start/$end/$engine"
    fun result(feature: String, name: String, elapsedMs: Long, lib: String = "") =
        "result/$feature/${Uri.encode(name)}/$elapsedMs?lib=${Uri.encode(lib)}"
}

// Open a kept library item: extractions reopen on the YouTube screen, vocal splits on Results.
private fun openLibraryItem(navController: androidx.navigation.NavController, item: com.vaibhawmishra.voela.ui.home.RecentAudio) {
    if (item.type == com.vaibhawmishra.voela.ui.home.ProcessType.EXTRACTION) {
        navController.navigate(Routes.youtube(item.id))
    } else {
        navController.navigate(Routes.result(com.vaibhawmishra.voela.ui.trim.TrimFeature.VOCALS.key, item.title, 0L, item.id))
    }
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
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            val recents by homeViewModel.recents.collectAsStateWithLifecycle()
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { navController.navigate(Routes.feature(displayName(context, it), it.toString())) }
            }
            HomeScreen(
                recents = recents,
                onChooseFile = { picker.launch("audio/*") },
                onYouTubeUrl = { navController.navigate(Routes.youtube()) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onRecentClick = { openLibraryItem(navController, it) },
                onRecentDelete = { homeViewModel.delete(it.id) },
            )
        }
        composable(
            Routes.YOUTUBE,
            arguments = listOf(navArgument("lib") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            val lib = entry.arguments?.getString("lib").orEmpty()
            val viewModel: YouTubeViewModel = viewModel(factory = YouTubeViewModel.factory(lib))
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
            val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LibraryScreen(
                uiState = state,
                onBack = navController::popBackStack,
                onItemClick = { openLibraryItem(navController, it) },
                onStartSelection = viewModel::startSelection,
                onEnterSelection = viewModel::enterSelection,
                onToggle = viewModel::toggle,
                onSelectAll = viewModel::selectAll,
                onExitSelection = viewModel::exitSelection,
                onDeleteSelected = viewModel::deleteSelected,
                onClearAll = viewModel::clearAll,
            )
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
                onEngineChange = viewModel::onEngineChange,
                onProceed = {
                    navController.navigate(Routes.process(state.feature.key, name, source, state.startMs, state.endMs, state.engine.name.lowercase()))
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
                navArgument("engine") { type = NavType.StringType },
            ),
        ) { entry ->
            val feature = TrimFeature.from(entry.arguments?.getString("feature"))
            val name = entry.arguments?.getString("name").orEmpty()
            val source = entry.arguments?.getString("source").orEmpty()
            val start = entry.arguments?.getLong("start") ?: 0L
            val end = entry.arguments?.getLong("end") ?: 0L
            val engine = entry.arguments?.getString("engine").orEmpty()
            val viewModel: SplitViewModel = viewModel(factory = SplitViewModel.factory(feature, source, start, end, engine, name))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isComplete) {
                if (state.isComplete) {
                    navController.navigate(Routes.result(feature.key, name, state.elapsedMs, state.libraryId)) {
                        popUpTo(Routes.PROCESS) { inclusive = true }
                    }
                }
            }
            LaunchedEffect(state.failed) {
                if (state.failed) navController.popBackStack()
            }
            SplitScreen(uiState = state, onCancel = { viewModel.cancel(); navController.popBackStack() })
        }
        composable(
            Routes.RESULT,
            arguments = listOf(
                navArgument("feature") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("elapsed") { type = NavType.LongType },
                navArgument("lib") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val name = entry.arguments?.getString("name").orEmpty()
            val elapsed = entry.arguments?.getLong("elapsed") ?: 0L
            val lib = entry.arguments?.getString("lib").orEmpty()
            val viewModel: ResultViewModel = viewModel(factory = ResultViewModel.factory(name, elapsed, lib))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ResultScreen(
                uiState = state,
                onBack = navController::popBackStack,
                onPlayPause = viewModel::onPlayPause,
                onSeek = viewModel::onSeek,
                onSave = viewModel::onSave,
                onMessageShown = viewModel::onMessageShown,
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
