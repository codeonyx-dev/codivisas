package com.codeonyx.bcvaqui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.codeonyx.bcvaqui.ui.donde.DondeScreen
import com.codeonyx.bcvaqui.ui.home.HomeScreen
import com.codeonyx.bcvaqui.ui.settings.SettingsScreen
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight
import com.codeonyx.bcvaqui.ui.tools.AlcanzaScreen
import com.codeonyx.bcvaqui.ui.tools.ComisionScreen
import com.codeonyx.bcvaqui.ui.tools.CompartirToolScreen
import com.codeonyx.bcvaqui.ui.tools.DividirScreen
import com.codeonyx.bcvaqui.ui.tools.HistorialScreen
import com.codeonyx.bcvaqui.ui.tools.MetaScreen
import com.codeonyx.bcvaqui.ui.tools.ToolRoutes
import com.codeonyx.bcvaqui.ui.tools.ToolsHubScreen
import com.codeonyx.bcvaqui.ui.tools.VariacionScreen
import com.codeonyx.bcvaqui.ui.tools.agenda.AgendaPagosScreen
import com.codeonyx.bcvaqui.ui.tools.toolSubtitle
import com.codeonyx.bcvaqui.ui.tools.toolTitle

sealed class AppRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : AppRoute("home", "Inicio", Icons.Filled.Home, Icons.Outlined.Home)
    data object Tools : AppRoute(ToolRoutes.GRAPH, "Herramientas", Icons.Filled.Build, Icons.Outlined.Build)
    data object Settings : AppRoute("settings", "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val bottomRoutes = listOf(AppRoute.Home, AppRoute.Tools, AppRoute.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BcvAquiNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentBottom = bottomRoutes.firstOrNull { route ->
        currentDestination?.hierarchy?.any { d -> d.route == route.route } == true
    }
    val isHome = currentBottom == AppRoute.Home

    val toolLeaf = currentDestination?.route?.takeIf {
        it != ToolRoutes.HUB && it != ToolRoutes.GRAPH && currentBottom == AppRoute.Tools
    }
    val showToolBack = toolLeaf != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (showToolBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    title = {
                        when (currentBottom) {
                            AppRoute.Home -> {
                                Column {
                                    Text(
                                        text = "BCV Aquí",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Tasas oficiales del Banco Central",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            AppRoute.Tools -> {
                                Column {
                                    Text(
                                        text = toolTitle(toolLeaf),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = toolSubtitle(toolLeaf),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = currentBottom?.label ?: "BCV Aquí",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BcvBlue,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomRoutes.forEach { route ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) route.selectedIcon else route.unselectedIcon,
                                    contentDescription = route.label
                                )
                            },
                            label = { Text(route.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BcvBlue,
                                selectedTextColor = BcvBlue,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(
                        if (isHome) Color.Transparent else MaterialTheme.colorScheme.background
                    )
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(AppRoute.Home.route) { HomeScreen() }
                    navigation(
                        startDestination = ToolRoutes.HUB,
                        route = ToolRoutes.GRAPH
                    ) {
                        composable(ToolRoutes.HUB) {
                            ToolsHubScreen(
                                onOpenTool = { tool ->
                                    navController.navigate(tool)
                                }
                            )
                        }
                        composable(ToolRoutes.DONDE) { DondeScreen() }
                        composable(ToolRoutes.ALCANZA) { AlcanzaScreen() }
                        composable(ToolRoutes.COMISION) { ComisionScreen() }
                        composable(ToolRoutes.VARIACION) { VariacionScreen() }
                        composable(ToolRoutes.HISTORIAL) { HistorialScreen() }
                        composable(ToolRoutes.META) { MetaScreen() }
                        composable(ToolRoutes.DIVIDIR) { DividirScreen() }
                        composable(ToolRoutes.COMPARTIR) { CompartirToolScreen() }
                        composable(ToolRoutes.AGENDA) { AgendaPagosScreen() }
                    }
                    composable(AppRoute.Settings.route) { SettingsScreen() }
                }
            }
        }
    }
}
