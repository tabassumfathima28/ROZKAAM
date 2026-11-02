package com.rozkaam.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozkaam.app.ui.navigation.Screen
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily

data class RozkaamNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val workerNavItems = listOf(
    RozkaamNavItem(Screen.WorkerHome.route, "Home", Icons.Outlined.Home),
    RozkaamNavItem(Screen.WorkerEarnings.route, "Earnings", Icons.Outlined.CurrencyRupee),
    RozkaamNavItem(Screen.WorkerTasks.route, "Tasks", Icons.AutoMirrored.Outlined.Assignment),
    RozkaamNavItem(Screen.Profile.route, "Profile", Icons.Outlined.Person)
)

val posterNavItems = listOf(
    RozkaamNavItem(Screen.PosterHome.route, "Home", Icons.Outlined.Home),
    RozkaamNavItem(Screen.PosterTasks.route, "Tasks", Icons.AutoMirrored.Outlined.Assignment),
    RozkaamNavItem(Screen.PosterWallet.route, "Wallet", Icons.Outlined.AccountBalanceWallet),
    RozkaamNavItem(Screen.Profile.route, "Profile", Icons.Outlined.Person)
)

/**
 * Shared Component 6: Bottom Bar
 * Supports Worker mode (Home, Earnings, Tasks, Profile) and Poster mode (Home, Tasks, Wallet, Profile).
 */
@Composable
fun RozkaamBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    isWorkerMode: Boolean,
    modifier: Modifier = Modifier
) {
    val items = if (isWorkerMode) workerNavItems else posterNavItems

    NavigationBar(
        modifier = modifier.height(68.dp),
        containerColor = BackgroundLight,
        tonalElevation = 4.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onNavigate(item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OldBurgundy,
                    selectedTextColor = OldBurgundy,
                    unselectedIconColor = OldBurgundy.copy(alpha = 0.5f),
                    unselectedTextColor = OldBurgundy.copy(alpha = 0.5f),
                    indicatorColor = Buttermilk
                )
            )
        }
    }
}
