package com.ochre.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ochre.R
import com.ochre.presentation.common.OchreColors

private sealed class NavIcon {
    data class Vector(val icon: ImageVector) : NavIcon()
    data class Res(val resId: Int) : NavIcon()
}

private data class NavItem(val screen: Screen, val label: String, val icon: NavIcon)

private val navItems = listOf(
    NavItem(Screen.Home,     "Home",     NavIcon.Res(R.drawable.ic_dog_run)),
    NavItem(Screen.Walk,     "Walk",     NavIcon.Vector(Icons.Default.DirectionsWalk)),
    NavItem(Screen.Calendar, "Calendar", NavIcon.Vector(Icons.Default.CalendarMonth)),
    NavItem(Screen.Training, "Training", NavIcon.Vector(Icons.Default.FitnessCenter)),
    NavItem(Screen.Medical,  "Medical",  NavIcon.Vector(Icons.Default.LocalHospital)),
    NavItem(Screen.Settings, "Settings", NavIcon.Vector(Icons.Default.Settings)),
)

@Composable
fun OchreNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = OchreColors.Background,
        contentColor = OchreColors.TextSecondary,
        tonalElevation = 0.dp
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    when (val ic = item.icon) {
                        is NavIcon.Vector -> Icon(imageVector = ic.icon, contentDescription = item.label)
                        is NavIcon.Res    -> Icon(painter = painterResource(ic.resId), contentDescription = item.label)
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) OchreColors.Accent else OchreColors.TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = OchreColors.Accent,
                    unselectedIconColor = OchreColors.TextSecondary,
                    selectedTextColor = OchreColors.Accent,
                    unselectedTextColor = OchreColors.TextSecondary
                )
            )
        }
    }
}
