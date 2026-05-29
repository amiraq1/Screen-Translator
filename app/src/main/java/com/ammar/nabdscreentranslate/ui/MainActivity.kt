package com.ammar.nabdscreentranslate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ammar.nabdscreentranslate.core.permissions.PermissionHelper
import com.ammar.nabdscreentranslate.overlay.FloatingButtonService
import com.ammar.nabdscreentranslate.ui.home.HomeViewModel
import com.ammar.nabdscreentranslate.ui.navigation.AppNavigation
import com.ammar.nabdscreentranslate.ui.theme.NabdTheme

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionHelper.canDrawOverlays(this)) {
            // Permission granted, start floating service
            startFloatingService()
        } else {
            Toast.makeText(
                this,
                "نحتاج هذه الصلاحية لعرض زر الترجمة العائم فوق التطبيقات الأخرى.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Continue regardless - notification permission is not critical
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (PermissionHelper.needsNotificationPermission()) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            NabdTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val homeViewModel: HomeViewModel = viewModel()

                    // Update overlay permission state
                    LaunchedEffect(Unit) {
                        homeViewModel.setOverlayPermission(PermissionHelper.canDrawOverlays(this@MainActivity))
                    }

                    AppNavigation(
                        homeViewModel = homeViewModel,
                        onToggleFloating = {
                            val state = homeViewModel.uiState.value
                            if (state.isFloatingActive) {
                                stopFloatingService()
                                homeViewModel.setFloatingActive(false)
                            } else {
                                if (PermissionHelper.canDrawOverlays(this@MainActivity)) {
                                    startFloatingService()
                                    homeViewModel.setFloatingActive(true)
                                } else {
                                    requestOverlayPermission()
                                }
                            }
                        },
                        onRequestOverlayPermission = { requestOverlayPermission() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh overlay permission state when returning from settings
    }

    private fun requestOverlayPermission() {
        val intent = PermissionHelper.getOverlayPermissionIntent(this)
        overlayPermissionLauncher.launch(intent)
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingButtonService::class.java).apply {
            action = FloatingButtonService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingButtonService::class.java).apply {
            action = FloatingButtonService.ACTION_STOP
        }
        startService(intent)
    }
}
