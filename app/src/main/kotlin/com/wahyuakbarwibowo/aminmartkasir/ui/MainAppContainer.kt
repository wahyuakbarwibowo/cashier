package com.wahyuakbarwibowo.aminmartkasir.ui

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.components.MoreMenuSheet
import com.wahyuakbarwibowo.aminmartkasir.ui.components.primaryMenuItems
import com.wahyuakbarwibowo.aminmartkasir.ui.components.secondaryMenuItems
import com.wahyuakbarwibowo.aminmartkasir.ui.navigation.AppNavigation
import com.wahyuakbarwibowo.aminmartkasir.ui.navigation.Screen
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DashboardViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory
) {
    val navController = rememberNavController()
    var showMoreMenu by remember { mutableStateOf(false) }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val primaryRoutes = remember {
        primaryMenuItems.map { it.route }.toSet()
    }
    val secondaryRoutes = remember {
        secondaryMenuItems.map { it.route }.toSet()
    }
    val topLevelRoutes = remember {
        primaryRoutes + secondaryRoutes
    }
    val showBottomBar = currentRoute in topLevelRoutes
    val moreMenuBadgeCount = remember(settingsState.shopProfile, settingsState.paymentMethods) {
        var issues = 0
        val profile = settingsState.shopProfile

        val isShopNameMissing = profile?.name.isNullOrBlank()
        val isPhoneMissing = profile?.phoneNumber.isNullOrBlank()
        val hasPaymentMethods = settingsState.paymentMethods.isNotEmpty()

        if (isShopNameMissing || isPhoneMissing) {
            issues += 1
        }
        if (!hasPaymentMethods) {
            issues += 1
        }

        issues
    }

    fun navigateTo(route: String) {
        if (route == Screen.Dashboard.route) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    primaryMenuItems.forEach { item ->
                        val badgeCount = when (item.route) {
                            Screen.Products.route -> dashboardState.lowStockCount
                            Screen.SalesHistory.route -> dashboardState.totalSales
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navigateTo(item.route) },
                            icon = {
                                BottomNavIcon(
                                    icon = item.icon,
                                    contentDescription = item.title,
                                    badgeCount = badgeCount
                                )
                            },
                            label = { Text(item.title) }
                        )
                    }
                    NavigationBarItem(
                        selected = currentRoute in secondaryRoutes,
                        onClick = { showMoreMenu = true },
                        icon = {
                            BottomNavIcon(
                                icon = Icons.Default.MoreHoriz,
                                contentDescription = "Lainnya",
                                badgeCount = moreMenuBadgeCount
                            )
                        },
                        label = { Text("Lainnya") }
                    )
                }
            }
        }
    ) {
        AppNavigation(
            navController = navController,
            viewModelFactory = viewModelFactory,
            onOpenDrawer = {
                showMoreMenu = true
            }
        )
    }

    if (showMoreMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            MoreMenuSheet(
                currentRoute = currentRoute,
                settingsBadgeCount = moreMenuBadgeCount,
                onNavigate = { route ->
                    navigateTo(route)
                    showMoreMenu = false
                },
                onDismiss = { showMoreMenu = false }
            )
        }
    }
}

@Composable
private fun BottomNavIcon(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int = 0
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    } else {
        Icon(icon, contentDescription = contentDescription)
    }
}
