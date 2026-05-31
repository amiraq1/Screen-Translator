package com.ammar.nabdscreentranslate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ammar.nabdscreentranslate.ui.history.HistoryScreen
import com.ammar.nabdscreentranslate.ui.history.HistoryViewModel
import com.ammar.nabdscreentranslate.ui.home.HomeScreen
import com.ammar.nabdscreentranslate.ui.home.HomeViewModel
import com.ammar.nabdscreentranslate.ui.settings.SettingsScreen
import com.ammar.nabdscreentranslate.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    homeViewModel: HomeViewModel = viewModel(),
    onToggleFloating: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                uiState = homeUiState,
                onToggleFloating = onToggleFloating,
                onSourceLangChanged = homeViewModel::setSourceLang,
                onTargetLangChanged = homeViewModel::setTargetLang,
                onDownloadModels = homeViewModel::downloadModels,
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onRequestOverlayPermission = onRequestOverlayPermission
            )
        }

        composable(Routes.HISTORY) {
            val historyViewModel: HistoryViewModel = viewModel()
            val historyItems by historyViewModel.historyItems.collectAsState()
            val searchQuery by historyViewModel.searchQuery.collectAsState()

            HistoryScreen(
                historyItems = historyItems,
                searchQuery = searchQuery,
                onSearchQueryChanged = historyViewModel::setSearchQuery,
                onDeleteItem = historyViewModel::deleteItem,
                onDeleteAll = historyViewModel::deleteAll,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            SettingsScreen(
                uiState = settingsState,
                onOverlayOpacityChanged = settingsViewModel::setOverlayOpacity,
                onSaveHistoryChanged = settingsViewModel::setSaveHistory,
                onDarkModeChanged = settingsViewModel::setDarkMode,
                onVibrateChanged = settingsViewModel::setVibrateOnTranslate,
                onDisplayModeChanged = settingsViewModel::setDisplayMode,
                onDeclutterChanged = settingsViewModel::setDeclutterOverlay,
                onPolishArabicChanged = settingsViewModel::setPolishArabic,
                onLiveTranslationChanged = settingsViewModel::setLiveTranslation,
                onLiveIntervalChanged = settingsViewModel::setLiveInterval,
                onLiveOnlyOnChangeChanged = settingsViewModel::setLiveOnlyOnChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
