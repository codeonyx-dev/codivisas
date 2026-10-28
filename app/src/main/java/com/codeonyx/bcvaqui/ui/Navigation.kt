package com.codeonyx.bcvaqui.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.setValue
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
import com.codeonyx.bcvaqui.ui.tools.ComisionScreen
import com.codeonyx.bcvaqui.ui.tools.DividirScreen
import com.codeonyx.bcvaqui.ui.tools.HistorialScreen
import com.codeonyx.bcvaqui.ui.tools.ToolRoutes
import com.codeonyx.bcvaqui.ui.theme.BrandColors
import com.codeonyx.bcvaqui.ui.tools.IvaIgtfScreen
import com.codeonyx.bcvaqui.ui.tools.BrechaScreen
import com.codeonyx.bcvaqui.ui.tools.ToolsHubScreen
import com.codeonyx.bcvaqui.ui.home.CalculatorPanel
import com.codeonyx.bcvaqui.ui.tools.agenda.AgendaPagosScreen
import com.codeonyx.bcvaqui.ui.tools.toolSubtitle
import com.codeonyx.bcvaqui.ui.tools.toolTitle
import com.codeonyx.bcvaqui.ui.tour.TourKeys
import com.codeonyx.bcvaqui.ui.tour.tourTarget
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val viewModel = appViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentBottom = bottomRoutes.firstOrNull { route ->
        currentDestination?.hierarchy?.any { d -> d.route == route.route } == true
    }
    val isHome = currentBottom == AppRoute.Home
    val tourTargets = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val tourRequested by viewModel.tourRequested.collectAsStateWithLifecycle()
    val showTour = tourRequested || !prefs.hasSeenOnboarding
    var tourFocus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val lite = prefs.liteMode && !showTour
    val visibleRoutes = if (lite) listOf(AppRoute.Home, AppRoute.Settings) else bottomRoutes

    val toolLeaf = currentDestination?.route?.takeIf {
        it != ToolRoutes.HUB && it != ToolRoutes.GRAPH && currentBottom == AppRoute.Tools
    }
    val showToolBack = toolLeaf != null
    val titleKey = when (currentBottom) {
        AppRoute.Home -> "home"
        AppRoute.Tools -> toolLeaf ?: "tools"
        AppRoute.Settings -> "settings"
        null -> "app"
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.codeonyx.bcvaqui.ui.tour.LocalTourTargets provides tourTargets,
        com.codeonyx.bcvaqui.ui.tour.LocalTourFocus provides tourFocus
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        AnimatedVisibility(
                            visible = showToolBack,
                            enter = fadeIn() + slideInVertically { -it / 2 },
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = BrandColors.onTopBar
                                )
                            }
                        }
                    },
                    title = {
                        AnimatedContent(
                            targetState = titleKey,
                            transitionSpec = { Motion.titleTransform() },
                            label = "topTitle"
                        ) { key ->
                            when {
                                key == "home" -> {
                                    Column {
                                        Text(
                                            text = "Codivisas",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "Tasas BCV y brecha P2P",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandColors.onTopBarMuted
                                        )
                                    }
                                }
                                key == "settings" -> {
                                    Text(
                                        text = "Ajustes",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                else -> {
                                    Column {
                                        Text(
                                            text = stringResource(toolTitle(toolLeaf)),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = stringResource(toolSubtitle(toolLeaf)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandColors.onTopBarMuted
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandColors.topBar,
                        titleContentColor = BrandColors.onTopBar,
                        navigationIconContentColor = BrandColors.onTopBar,
                        actionIconContentColor = BrandColors.onTopBar
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    visibleRoutes.forEach { route ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
                        NavigationBarItem(
                            modifier = when (route) {
                                AppRoute.Tools -> Modifier.tourTarget(TourKeys.NAV_TOOLS)
                                AppRoute.Settings -> Modifier.tourTarget(TourKeys.NAV_SETTINGS)
                                else -> Modifier
                            },
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
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
                    .tourTarget(if (currentBottom == AppRoute.Settings) TourKeys.SETTINGS_CONTENT else if (currentBottom == AppRoute.Tools) TourKeys.TOOLS_CONTENT else "content")
                    .background(
                        if (isHome) Color.Transparent else MaterialTheme.colorScheme.background
                    )
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home.route,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { Motion.tabEnter() },
                    exitTransition = { Motion.tabExit() },
                    popEnterTransition = { Motion.tabEnter() },
                    popExitTransition = { Motion.tabExit() }
                ) {
                    composable(AppRoute.Home.route) {
                        if (lite) com.codeonyx.bcvaqui.ui.home.LiteHomeScreen(viewModel) else HomeScreen(viewModel = viewModel)
                    }
                    navigation(
                        startDestination = ToolRoutes.HUB,
                        route = ToolRoutes.GRAPH
                    ) {
                        composable(
                            route = ToolRoutes.HUB,
                            enterTransition = { Motion.tabEnter() },
                            exitTransition = { Motion.toolExit() },
                            popEnterTransition = { Motion.toolPopEnter() },
                            popExitTransition = { Motion.tabExit() }
                        ) {
                            ToolsHubScreen(onOpenTool = { navController.navigate(it) })
                        }
                        toolComposable(ToolRoutes.DONDE) { DondeScreen(viewModel) }
                        toolComposable(ToolRoutes.COMISION) { ComisionScreen() }
                        toolComposable(ToolRoutes.CALCULADORA) { CalculatorPanel(viewModel) }
                        toolComposable(ToolRoutes.HISTORIAL) { HistorialScreen(viewModel) }
                        toolComposable(ToolRoutes.DIVIDIR) { DividirScreen(viewModel) }
                        toolComposable(ToolRoutes.AGENDA) { AgendaPagosScreen(viewModel) }
                        toolComposable(ToolRoutes.IVA_IGTF) { IvaIgtfScreen(viewModel) }
                        toolComposable(ToolRoutes.BRECHA) { BrechaScreen(viewModel) }
                        toolComposable(ToolRoutes.MERCADO) { com.codeonyx.bcvaqui.ui.tools.MercadoScreen(viewModel) }
                        toolComposable(ToolRoutes.DEUDAS) { com.codeonyx.bcvaqui.ui.tools.finance.DeudasScreen(viewModel) }
                        toolComposable(ToolRoutes.AHORROS) { com.codeonyx.bcvaqui.ui.tools.finance.AhorrosScreen(viewModel) }
                        toolComposable(ToolRoutes.COMERCIANTE) { com.codeonyx.bcvaqui.ui.tools.ComercianteScreen(viewModel) }
                    }
                    composable(AppRoute.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onOpenPaymentAgenda = {
                                navController.navigate(ToolRoutes.AGENDA)
                            }
                        )
                    }
                }
            }
        }
        if (showTour) {
            com.codeonyx.bcvaqui.ui.tour.TourOverlay(
                targets = tourTargets,
                onNavigate = { r ->
                    val dest = when (r) { "tools" -> AppRoute.Tools.route; "settings" -> AppRoute.Settings.route; else -> AppRoute.Home.route }
                    if (currentBottom?.route != dest) navController.navigate(dest) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                },
                onFocus = { tourFocus = it },
                onLite = {
                    viewModel.setLiteMode(true)
                    viewModel.completeOnboarding()
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true
                    }
                },
                onFinish = {
                    viewModel.completeOnboarding()
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true
                    }
                }
            )
        }
    }
    }
}

private fun androidx.navigation.NavGraphBuilder.toolComposable(
    route: String,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        enterTransition = { Motion.toolEnter() },
        exitTransition = { Motion.toolExit() },
        popEnterTransition = { Motion.toolPopEnter() },
        popExitTransition = { Motion.toolPopExit() }
    ) {
        content()
    }
}
