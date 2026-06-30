package com.umbrel.android.navigation

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object AppStore : Screen("app_store")
    data object AppDetail : Screen("app_detail/{appId}") {
        fun createRoute(appId: String) = "app_detail/$appId"
    }
    data object MyApps : Screen("my_apps")
    data object SystemStatus : Screen("system_status")
    data object Files : Screen("files/{path}") {
        fun createRoute(path: String = "Home") = "files/$path"
    }
    data object FilePreview : Screen("file_preview/{path}") {
        fun createRoute(path: String) = "file_preview/$path"
    }
    data object Backups : Screen("backups")
    data object Wifi : Screen("wifi")
    data object Settings : Screen("settings")
    data object Notifications : Screen("notifications")
    data object QrScanner : Screen("qr_scanner")
}
