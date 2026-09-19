package com.mawaqit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.mawaqit.app.R
import com.mawaqit.app.ui.home.HomeScreen
import com.mawaqit.app.ui.quran.QuranListScreen
import com.mawaqit.app.ui.quran.SurahDetailScreen
import com.mawaqit.app.ui.theme.PrimaryGold

/**
 * Bottom navigation with 4 top-level destinations (PHASE_4 guidebook).
 * DESIGN.md §4: floating white pill bar; active tab = gold filled circle
 * behind a white icon. Labels kept under icons (DESIGN.md §9 justified
 * deviation from the kit's icon-only nav). Quran/Qibla/Settings are
 * placeholders until PHASE_6/7/8.
 */
private data class Tab(
    val route: String,
    val labelRes: Int,
    val filled: ImageVector,
    val outlined: ImageVector
)

private val TABS = listOf(
    Tab("home", R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    Tab("quran", R.string.tab_quran, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    Tab("qibla", R.string.tab_qibla, Icons.Filled.Explore, Icons.Outlined.Explore),
    Tab("settings", R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MawaqitNavGraph(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Reading screen is immersive: hide the pill bar on surah/{number} (DESIGN.md §4/§5).
    val showBottomBar = currentRoute?.startsWith("surah/") != true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TABS.forEach { tab ->
                    val selected = currentRoute == tab.route
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selected) PrimaryGold else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.filled else tab.outlined,
                                contentDescription = stringResource(tab.labelRes),
                                tint = if (selected) Color.White else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen() }
            composable("quran") {
                QuranListScreen(onSurahClick = { number -> navController.navigate("surah/$number") })
            }
            composable("qibla") { ComingSoonScreen(stringResource(R.string.tab_qibla), 7) }
            composable("settings") { ComingSoonScreen(stringResource(R.string.tab_settings), 8) }
            composable(
                route = "surah/{number}",
                arguments = listOf(navArgument("number") { type = NavType.IntType })
            ) {
                SurahDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String, phase: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.coming_in_phase, phase),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
