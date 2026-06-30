package com.umbrel.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.umbrel.android.core.auth.AuthViewModel
import com.umbrel.android.ui.screens.appstore.AppStoreScreen
import com.umbrel.android.ui.screens.appstore.AppDetailScreen
import com.umbrel.android.ui.screens.apps.MyAppsScreen
import com.umbrel.android.ui.screens.backups.BackupsScreen
import com.umbrel.android.ui.screens.dashboard.DashboardScreen
import com.umbrel.android.ui.screens.files.FilesScreen
import com.umbrel.android.ui.screens.files.FilePreviewScreen
import com.umbrel.android.ui.screens.login.LoginScreen
import com.umbrel.android.ui.screens.notifications.NotificationsScreen
import com.umbrel.android.ui.screens.qrscanner.QrScannerScreen
import com.umbrel.android.ui.screens.settings.SettingsScreen
import com.umbrel.android.ui.screens.setup.SetupScreen
import com.umbrel.android.ui.screens.system.SystemStatusScreen
import com.umbrel.android.ui.screens.wifi.WifiScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard),
    BottomNavItem("App Store", Icons.Default.Store, Screen.AppStore),
    BottomNavItem("My Apps", Icons.Default.Apps, Screen.MyApps),
    BottomNavItem("Files", Icons.Default.Folder, Screen.Files),
    BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmbrelNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = when {
                authState is com.umbrel.android.core.auth.AuthState.NeedsUrl -> Screen.Setup.route
                authState is com.umbrel.android.core.auth.AuthState.NeedsLogin -> Screen.Login.route
                else -> Screen.Dashboard.route
            },
            modifier = Modifier.padding(innerPadding),
        ) {
            // Auth screens
            composable(
                route = Screen.Setup.route + "?scannedUrl={scannedUrl}",
                arguments = listOf(
                    navArgument("scannedUrl") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { backStackEntry ->
                val scannedUrl = backStackEntry.arguments?.getString("scannedUrl")
                SetupScreen(
                    onServerConfigured = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    },
                    onScanQr = {
                        navController.navigate(Screen.QrScanner.route)
                    },
                    initialScannedUrl = if (scannedUrl.isNullOrBlank()) null else scannedUrl,
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                )
            }

            // Main screens
            composable(Screen.Dashboard.route) {
                DashboardScreen(navController = navController)
            }
            composable(Screen.AppStore.route) {
                AppStoreScreen(navController = navController)
            }
            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
                AppDetailScreen(appId = appId, navController = navController)
            }
            composable(Screen.MyApps.route) {
                MyAppsScreen(navController = navController)
            }
            composable(Screen.SystemStatus.route) {
                SystemStatusScreen(navController = navController)
            }
            composable(
                route = Screen.Files.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "Home" }),
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: "Home"
                FilesScreen(initialPath = path)
            }
            composable(
                route = Screen.FilePreview.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType }),
            ) { backStackEntry ->
                val filePath = backStackEntry.arguments?.getString("path") ?: return@composable
                FilePreviewScreen(filePath = filePath, navController = navController)
            }
            composable(Screen.Backups.route) {
                BackupsScreen()
            }
            composable(Screen.Wifi.route) {
                WifiScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen()
            }
            composable(Screen.QrScanner.route) {
                QrScannerScreen(
                    navController = navController,
                    onUrlScanned = { url ->
                        navController.navigate(
                            Screen.Setup.route + "?scannedUrl=${url}"
                        ) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
