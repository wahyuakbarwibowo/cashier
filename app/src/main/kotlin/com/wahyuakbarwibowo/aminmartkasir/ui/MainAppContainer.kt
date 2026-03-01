package com.wahyuakbarwibowo.aminmartkasir.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.ui.components.AppDrawer
import com.wahyuakbarwibowo.aminmartkasir.ui.navigation.AppNavigation
import kotlinx.coroutines.launch

@Composable
fun MainAppContainer(
    viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        AppNavigation(
            navController = navController,
            viewModelFactory = viewModelFactory,
            onOpenDrawer = {
                scope.launch { drawerState.open() }
            }
        )
    }
}
