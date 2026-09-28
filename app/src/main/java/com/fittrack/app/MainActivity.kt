package com.fittrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fittrack.app.ui.navigation.NavGraph
import com.fittrack.app.ui.navigation.Screen
import com.fittrack.app.ui.theme.FitTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FitTrackApp
        val appModule = app.appModule
        val proManager = app.proManager

        setContent {
            FitTrackTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Show bottom nav only on main screens
                val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                Screen.bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            screen.icon?.let { Icon(it, contentDescription = screen.title) }
                                        },
                                        label = { Text(screen.title) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            // Always issue navigate(). launchSingleTop + restoreState
                                            // make tapping the already-selected tab a cheap no-op,
                                            // and dropping the route-equality guard avoids issues
                                            // when currentRoute lags a frame behind a recent push.
                                            navController.navigate(screen.route) {
                                                // popUpTo the graph's actual start destination ID
                                                // rather than a hardcoded route string — survives
                                                // start-destination changes and is the pattern Google
                                                // recommends for bottom-nav setups.
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    // Apply the Scaffold's content padding so screens don't render
                    // underneath the bottom nav bar.
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavGraph(
                            navController = navController,
                            appModule = appModule,
                            proManager = proManager
                        )
                    }
                }
            }
        }
    }
}
