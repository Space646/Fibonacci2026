package com.fibonacci.fibohealth.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*
import com.fibonacci.fibohealth.ui.activity.ActivityScreen
import com.fibonacci.fibohealth.ui.dashboard.DashboardScreen
import com.fibonacci.fibohealth.ui.device.DeviceScreen
import com.fibonacci.fibohealth.ui.foodlog.FoodLogScreen
import com.fibonacci.fibohealth.ui.profile.ProfileScreen

enum class Destination(val label: String, val icon: ImageVector, val route: String) {
    Home    ("Home",    Icons.Rounded.Home,           "home"),
    Log     ("Log",     Icons.Rounded.RestaurantMenu, "log"),
    Activity("Activity",Icons.Rounded.Bolt,           "activity"),
    Profile ("Profile", Icons.Rounded.Person,         "profile"),
    Device  ("Device",  Icons.Rounded.Bluetooth,      "device"),
}

@Composable
fun FiboHealthNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries.forEach { dest ->
                item(
                    selected = currentRoute == dest.route,
                    onClick  = {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon  = { Icon(dest.icon, dest.label) },
                    label = { Text(dest.label) }
                )
            }
        }
    ) {
        NavHost(navController, startDestination = Destination.Home.route) {
            composable(Destination.Home.route)     { DashboardScreen() }
            composable(Destination.Log.route)      { FoodLogScreen() }
            composable(Destination.Activity.route) { ActivityScreen() }
            composable(Destination.Profile.route)  { ProfileScreen() }
            composable(Destination.Device.route)   { DeviceScreen() }
        }
    }
}
