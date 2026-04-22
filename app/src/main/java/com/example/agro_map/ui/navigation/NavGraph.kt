package com.example.agro_map.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.agro_map.AppContainer
import com.example.agro_map.ui.screens.addreport.AddReportScreen
import com.example.agro_map.ui.screens.leaderboard.LeaderboardScreen
import com.example.agro_map.ui.screens.login.LoginScreen
import com.example.agro_map.ui.screens.map.MapScreen
import com.example.agro_map.ui.screens.profile.ProfileScreen
import com.example.agro_map.ui.screens.register.RegisterScreen
import com.example.agro_map.ui.screens.reportdetail.ReportDetailScreen
import com.example.agro_map.ui.screens.reportlist.ReportListScreen
import kotlinx.serialization.Serializable

// Route definitions
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object MapRoute
@Serializable object ReportListRoute
@Serializable object LeaderboardRoute
@Serializable object ProfileRoute
@Serializable object AddReportRoute
@Serializable data class ReportDetailRoute(val reportId: String)

enum class BottomNavItem(val label: String, val icon: ImageVector, val route: Any) {
    MAP("Map", Icons.Default.Map, MapRoute),
    REPORTS("Reports", Icons.Default.List, ReportListRoute),
    LEADERBOARD("Ranking", Icons.Default.Leaderboard, LeaderboardRoute),
    PROFILE("Profile", Icons.Default.Person, ProfileRoute)
}

@Composable
fun AgroMapNavGraph(container: AppContainer, startDestination: Any) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination != null && BottomNavItem.entries.any {
        currentDestination.hasRoute(it.route::class)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hasRoute(item.route::class) == true,
                            onClick = {
                                navController.navigate(item.route) {
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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    container = container,
                    onLoginSuccess = {
                        navController.navigate(MapRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute)
                    }
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    container = container,
                    onRegisterSuccess = {
                        navController.navigate(MapRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable<MapRoute> {
                MapScreen(
                    container = container,
                    onAddReport = { navController.navigate(AddReportRoute) },
                    onReportClick = { reportId ->
                        navController.navigate(ReportDetailRoute(reportId))
                    }
                )
            }
            composable<AddReportRoute> {
                AddReportScreen(
                    container = container,
                    onReportAdded = { navController.popBackStack() }
                )
            }
            composable<ReportDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ReportDetailRoute>()
                ReportDetailScreen(
                    container = container,
                    reportId = route.reportId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<ReportListRoute> {
                ReportListScreen(
                    container = container,
                    onReportClick = { reportId ->
                        navController.navigate(ReportDetailRoute(reportId))
                    }
                )
            }
            composable<LeaderboardRoute> {
                LeaderboardScreen(container = container)
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    container = container,
                    onLogout = {
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
