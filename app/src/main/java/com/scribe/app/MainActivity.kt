package com.scribe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scribe.app.ui.ApiUsageScreen
import com.scribe.app.ui.CommandsScreen
import com.scribe.app.ui.DashboardScreen
import com.scribe.app.ui.HistoryScreen
import com.scribe.app.ui.SettingsScreen
import com.scribe.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScribeTheme {
                ScribeMainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Playground", Icons.Outlined.AutoAwesome)
    object Commands : Screen("commands", "Commands", Icons.AutoMirrored.Outlined.List)
    object History : Screen("history", "History", Icons.Outlined.History)
    object Settings : Screen("settings", "Engine", Icons.Outlined.Tune)
}

@Composable
fun ScribeMainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Commands, Screen.History, Screen.Settings)
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = ScribeBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = ScribeSurface.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0x26FFFFFF)
                    ),
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        items.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) ScribeIce else ScribeTextTertiary
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                        color = if (isSelected) ScribeTextPrimary else ScribeTextTertiary
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ScribeIce,
                                    selectedTextColor = ScribeTextPrimary,
                                    indicatorColor = ScribeCobalt.copy(alpha = 0.25f),
                                    unselectedIconColor = ScribeTextTertiary,
                                    unselectedTextColor = ScribeTextTertiary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Commands.route) { CommandsScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
            composable("api_usage/{keyIndex}") { backStackEntry ->
                val keyIndex = backStackEntry.arguments?.getString("keyIndex")?.toIntOrNull() ?: 0
                ApiUsageScreen(navController = navController, keyIndex = keyIndex)
            }
        }
    }
}