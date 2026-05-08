package com.ochre.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Walk : Screen("walk")
    object Food : Screen("food")
    object Calendar : Screen("calendar")
    object Stats : Screen("stats")
    object Medical : Screen("medical")
    object Settings : Screen("settings")
}
