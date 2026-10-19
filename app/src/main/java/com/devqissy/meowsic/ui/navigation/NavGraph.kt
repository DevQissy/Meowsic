package com.devqissy.meowsic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.devqissy.meowsic.data.viewmodel.MusicViewModel
import com.devqissy.meowsic.ui.screens.EqualizerScreen
import com.devqissy.meowsic.ui.screens.NowPlayingScreen
import com.devqissy.meowsic.ui.screens.PlaylistScreen
import com.devqissy.meowsic.ui.screens.SearchScreen
import com.devqissy.meowsic.ui.screens.SettingsScreen
import com.devqissy.meowsic.ui.screens.SongListScreen

sealed class Screen(val route: String) {
    data object SongList   : Screen("song_list")
    data object NowPlaying : Screen("now_playing")
    data object Playlists  : Screen("playlists")
    data object Search     : Screen("search")
    data object Equalizer  : Screen("equalizer")
    data object Settings   : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MusicViewModel
) {
    NavHost(navController = navController, startDestination = Screen.SongList.route) {

        composable(Screen.SongList.route) {
            SongListScreen(
                viewModel = viewModel,
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onPlaylistClick = { navController.navigate(Screen.Playlists.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onEqualizerClick = { navController.navigate(Screen.Equalizer.route) }
            )
        }

        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEqualizerClick = { navController.navigate(Screen.Equalizer.route) }
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = viewModel,
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Equalizer.route) {
            EqualizerScreen(
                audioEffectsManager = viewModel.audioEffectsManager,
                onReverbChange = { enabled, preset -> viewModel.setReverb(enabled, preset) },
                onResetAll = { viewModel.resetAll() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}