package com.example.strava2sheets.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.strava2sheets.ui.activities.ActivityListScreen
import com.example.strava2sheets.ui.athlete.AthleteScreen
import com.example.strava2sheets.ui.dashboard.DashboardScreen
import com.example.strava2sheets.ui.dashboard.DashboardViewModel
import com.example.strava2sheets.ui.document.DocumentScreen
import com.example.strava2sheets.ui.goals.GoalsScreen
import com.example.strava2sheets.ui.splash.SplashScreen
import com.example.strava2sheets.ui.stats.StatisticsScreen
import kotlinx.coroutines.launch

sealed class Screen(val title: String, val icon: ImageVector) {
    object Dashboard : Screen("Start", Icons.Default.Home)
    object Activities : Screen("Aktivität", Icons.AutoMirrored.Filled.List)
    object Statistics : Screen("Statistik", Icons.Default.Star)
    object Goals : Screen("Ziele", Icons.Default.Notifications)
    object Athlete : Screen("Benutzer", Icons.Default.Person)
    object Document : Screen("Dokument", Icons.Default.Info)
}

@Composable
fun MainAppNavigation(
    viewModel: DashboardViewModel
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            onTimeout = {
                showSplash = false
            }
        )
    } else {
        MainAppPager(viewModel = viewModel)
    }
}

@Composable
fun MainAppPager(
    viewModel: DashboardViewModel
) {
    val navBarScreens = listOf(
        Screen.Dashboard,
        Screen.Activities,
        Screen.Statistics,
        Screen.Goals,
        Screen.Athlete,
        Screen.Document
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { navBarScreens.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                navBarScreens.forEachIndexed { index, screen ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = {
                            Text(
                                text = screen.title,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        alwaysShowLabel = false, // Caption nur bei der aktiven Seite einblenden!
                        selected = isSelected,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(viewModel = viewModel)
                1 -> ActivityListScreen(viewModel = viewModel)
                2 -> StatisticsScreen(viewModel = viewModel)
                3 -> GoalsScreen(viewModel = viewModel)
                4 -> AthleteScreen(viewModel = viewModel)
                5 -> DocumentScreen(viewModel = viewModel)
            }
        }
    }
}
