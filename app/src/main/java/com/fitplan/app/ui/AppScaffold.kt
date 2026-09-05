package com.fitplan.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitplan.app.ui.screens.body.BodyScreen
import com.fitplan.app.ui.screens.day.DayPlanScreen
import com.fitplan.app.ui.screens.library.LibraryScreen
import com.fitplan.app.ui.screens.settings.SettingsScreen

enum class TopDest(val route: String, val label: String) {
    Day("day", "今日"),
    Library("library", "动作"),
    Body("body", "身体"),
    Settings("settings", "设置")
}

private data class NavItem(val dest: TopDest, val icon: ImageVector, val iconSelected: ImageVector)

private val items = listOf(
    NavItem(TopDest.Day, Icons.Outlined.Today, Icons.Filled.Today),
    NavItem(TopDest.Library, Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    NavItem(TopDest.Body, Icons.Outlined.MonitorWeight, Icons.Filled.MonitorWeight),
    NavItem(TopDest.Settings, Icons.Outlined.Settings, Icons.Filled.Settings)
)

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = currentRoute == item.dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.iconSelected else item.icon,
                                contentDescription = item.dest.label
                            )
                        },
                        label = { Text(item.dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Day.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopDest.Day.route) { DayPlanScreen() }
            composable(TopDest.Library.route) { LibraryScreen() }
            composable(TopDest.Body.route) { BodyScreen() }
            composable(TopDest.Settings.route) { SettingsScreen() }
        }
    }
}
