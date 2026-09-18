package com.scribe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scribe.app.ui.ApiUsageScreen
import com.scribe.app.ui.HistoryScreen
import com.scribe.app.ui.HubScreen
import com.scribe.app.ui.SettingsScreen
import com.scribe.app.ui.components.AuroraBackground
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
    object Hub      : Screen("hub",      "Hub",     Icons.Outlined.AutoAwesome)
    object History  : Screen("history",  "History", Icons.Outlined.History)
    object Settings : Screen("settings", "Engine",  Icons.Outlined.Tune)
}

@Composable
fun ScribeMainScreen() {
    val navController = rememberNavController()
    val items         = listOf(Screen.Hub, Screen.History, Screen.Settings)
    val haptic        = LocalHapticFeedback.current

    // Full-screen aurora gradient is the single background layer for the whole app.
    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,  // let aurora bleed through
            bottomBar = {
                // ── Floating glass pill nav bar ──────────────────────────────
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    // Glass pill container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            items.forEach { screen ->
                                val isSelected = currentRoute == screen.route
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState    = true
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector    = screen.icon,
                                            contentDescription = screen.title,
                                            tint    = if (isSelected) ScribeGlassCobalt else Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text       = screen.title,
                                            fontSize   = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color      = if (isSelected) ScribeGlassCobalt else Color.White.copy(alpha = 0.45f)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        // Cobalt active dot
                                        Box(
                                            modifier = Modifier
                                                .size(if (isSelected) 4.dp else 0.dp)
                                                .clip(CircleShape)
                                                .background(ScribeGlassCobalt)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = Screen.Hub.route,
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Hub.route)      { HubScreen(navController = navController) }
                composable(Screen.History.route)  { HistoryScreen() }
                composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
                composable("api_usage/{keyIndex}") { backStackEntry ->
                    val keyIndex = backStackEntry.arguments?.getString("keyIndex")?.toIntOrNull() ?: 0
                    ApiUsageScreen(navController = navController, keyIndex = keyIndex)
                }
            }
        }
    }
}