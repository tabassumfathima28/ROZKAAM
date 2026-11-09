package com.rozkaam.app.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozkaam.app.ui.navigation.Screen
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SurfaceLight

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
 * Adheres to SPEC.md Section A4 Design System.
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
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Subtle top border line to crisply separate bottom bar from content
                drawLine(
                    color = OldBurgundy.copy(alpha = 0.12f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        containerColor = SurfaceLight,
        tonalElevation = 6.dp,
        windowInsets = WindowInsets.navigationBars
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
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.2.sp
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OldBurgundy,
                    selectedTextColor = OldBurgundy,
                    unselectedIconColor = OldBurgundy.copy(alpha = 0.68f),
                    unselectedTextColor = OldBurgundy.copy(alpha = 0.72f),
                    indicatorColor = Buttermilk
                )
            )
        }
    }
}
