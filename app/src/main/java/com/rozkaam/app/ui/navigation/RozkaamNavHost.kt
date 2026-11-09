package com.rozkaam.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.rozkaam.app.data.model.User
import com.rozkaam.app.data.repository.FirestoreUserRepository
import com.rozkaam.app.ui.components.RozkaamBottomBar
import com.rozkaam.app.ui.screens.onboarding.LoginScreen
import com.rozkaam.app.ui.screens.onboarding.RoleSelectionScreen
import com.rozkaam.app.ui.screens.onboarding.SplashDestination
import com.rozkaam.app.ui.screens.onboarding.SplashScreen
import com.rozkaam.app.ui.screens.onboarding.WelcomeCarouselScreen
import com.rozkaam.app.ui.screens.poster.PosterAddMoneyScreen
import com.rozkaam.app.ui.screens.poster.PosterHomeScreen
import com.rozkaam.app.ui.screens.poster.PosterPostTaskScreen
import com.rozkaam.app.ui.screens.poster.PosterTaskDetailsScreen
import com.rozkaam.app.ui.screens.poster.PosterTasksScreen
import com.rozkaam.app.ui.screens.poster.PosterWalletScreen
import com.rozkaam.app.ui.screens.shared.NotificationsScreen
import com.rozkaam.app.ui.screens.shared.ProfileScreen
import com.rozkaam.app.ui.screens.shared.SosScreen
import com.rozkaam.app.ui.screens.worker.WorkerActiveJobScreen
import com.rozkaam.app.ui.screens.worker.WorkerEarningsScreen
import com.rozkaam.app.ui.screens.worker.WorkerHomeScreen
import com.rozkaam.app.ui.screens.worker.WorkerJobCompletedScreen
import com.rozkaam.app.ui.screens.worker.WorkerTaskDetailsScreen
import com.rozkaam.app.ui.screens.worker.WorkerTasksScreen
import com.rozkaam.app.ui.screens.worker.WorkerTaskQrScannerScreen
import com.rozkaam.app.ui.screens.worker.WorkerUploadProofScreen
import com.rozkaam.app.ui.screens.worker.WorkerWithdrawScreen
import com.rozkaam.app.ui.theme.BackgroundLight
import kotlinx.coroutines.launch

const val GRAPH_WORKER = "graph_worker"
const val GRAPH_POSTER = "graph_poster"

@Composable
fun RozkaamApp() {
    val navController = rememberNavController()
    var isWorkerMode by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val userRepository = remember { FirestoreUserRepository() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if bottom bar should be visible
    val showBottomBar = when (currentRoute) {
        Screen.WorkerHome.route,
        Screen.WorkerEarnings.route,
        Screen.WorkerTasks.route,
        Screen.PosterHome.route,
        Screen.PosterTasks.route,
        Screen.PosterWallet.route,
        Screen.Profile.route -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                RozkaamBottomBar(
                    currentRoute = currentRoute,
                    isWorkerMode = isWorkerMode,
                    onNavigate = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Onboarding / Before Login ---
            composable(Screen.Splash.route) {
                SplashScreen(
                    onDetermineDestination = { destination ->
                        when (destination) {
                            is SplashDestination.AuthenticatedHome -> {
                                isWorkerMode = destination.activeMode == User.MODE_WORKER
                                val target = if (isWorkerMode) GRAPH_WORKER else GRAPH_POSTER
                                navController.navigate(target) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                            is SplashDestination.RoleSelection -> {
                                navController.navigate(Screen.RoleSelection.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                            is SplashDestination.WelcomeCarousel -> {
                                navController.navigate(Screen.WelcomeCarousel.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            composable(Screen.WelcomeCarousel.route) {
                WelcomeCarouselScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLoginSuccess = { user ->
                        coroutineScope.launch {
                            val userDoc = userRepository.getUser(user.uid).getOrNull()
                            if (userDoc != null && userDoc.isProfileComplete) {
                                isWorkerMode = userDoc.activeMode == User.MODE_WORKER
                                val target = if (isWorkerMode) GRAPH_WORKER else GRAPH_POSTER
                                navController.navigate(target) {
                                    popUpTo(Screen.WelcomeCarousel.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.RoleSelection.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(
                    onComplete = { activeMode ->
                        isWorkerMode = activeMode == User.MODE_WORKER
                        val startDest = if (isWorkerMode) GRAPH_WORKER else GRAPH_POSTER
                        navController.navigate(startDest) {
                            popUpTo(Screen.WelcomeCarousel.route) { inclusive = true }
                        }
                    }
                )
            }

            // --- Worker Nested Navigation Graph ---
            navigation(
                startDestination = Screen.WorkerHome.route,
                route = GRAPH_WORKER
            ) {
                composable(Screen.WorkerHome.route) {
                    WorkerHomeScreen(
                        onNavigateToTaskDetails = { taskId ->
                            navController.navigate("worker/task_details?taskId=$taskId")
                        },
                        onNavigateToActiveJob = { navController.navigate(Screen.WorkerActiveJob.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToSos = { navController.navigate(Screen.Sos.route) }
                    )
                }

                composable(
                    route = "worker/task_details?taskId={taskId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("taskId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
                    WorkerTaskDetailsScreen(
                        taskId = taskId,
                        onNavigateBack = { navController.popBackStack() },
                        onAcceptTask = { acceptedTaskId ->
                            navController.navigate(Screen.WorkerUploadProof.createRoute(acceptedTaskId))
                        }
                    )
                }

                composable(
                    route = "worker/active_job?taskId={taskId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("taskId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    WorkerActiveJobScreen(
                        taskId = backStackEntry.arguments?.getString("taskId").orEmpty(),
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToUploadProof = { acceptedTaskId ->
                            navController.navigate(Screen.WorkerUploadProof.createRoute(acceptedTaskId))
                        },
                        onNavigateToJobCompleted = { navController.navigate(Screen.WorkerJobCompleted.route) }
                    )
                }

                composable(
                    route = "worker/upload_proof?taskId={taskId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("taskId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    WorkerUploadProofScreen(
                        taskId = backStackEntry.arguments?.getString("taskId").orEmpty(),
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToActiveJob = { acceptedTaskId ->
                            navController.navigate(Screen.WorkerActiveJob.createRoute(acceptedTaskId))
                        },
                        onNavigateToProofQrScanner = { submittedTaskId ->
                            navController.navigate(Screen.WorkerProofQrScanner.createRoute(submittedTaskId))
                        }
                    )
                }

                composable(
                    route = "worker/proof_qr_scanner?taskId={taskId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("taskId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    WorkerTaskQrScannerScreen(
                        taskId = backStackEntry.arguments?.getString("taskId").orEmpty(),
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTasks = {
                            navController.navigate(Screen.WorkerTasks.route) {
                                popUpTo(GRAPH_WORKER) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.WorkerJobCompleted.route) {
                    WorkerJobCompletedScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.WorkerEarnings.route) {
                    WorkerEarningsScreen(
                        onNavigateToWithdraw = { navController.navigate(Screen.WorkerWithdraw.route) }
                    )
                }

                composable(Screen.WorkerWithdraw.route) {
                    WorkerWithdrawScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.WorkerTasks.route) {
                    WorkerTasksScreen(
                        onNavigateToTaskDetails = { taskId ->
                            navController.navigate(Screen.WorkerTaskDetails.createRoute(taskId))
                        },
                        onNavigateToActiveJob = { taskId ->
                            navController.navigate(Screen.WorkerActiveJob.createRoute(taskId))
                        },
                        onNavigateToUploadProof = { taskId ->
                            navController.navigate(Screen.WorkerUploadProof.createRoute(taskId))
                        }
                    )
                }
            }

            // --- Poster Nested Navigation Graph ---
            navigation(
                startDestination = Screen.PosterHome.route,
                route = GRAPH_POSTER
            ) {
                composable(Screen.PosterHome.route) {
                    PosterHomeScreen(
                        onNavigateToPostTask = { navController.navigate(Screen.PosterPostTask.route) },
                        onNavigateToTaskDetails = { taskId ->
                            navController.navigate("poster/task_details?taskId=$taskId")
                        },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToSos = { navController.navigate(Screen.Sos.route) }
                    )
                }

                composable(Screen.PosterPostTask.route) {
                    PosterPostTaskScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddMoney = { navController.navigate(Screen.PosterAddMoney.route) }
                    )
                }

                composable(
                    route = "poster/task_details?taskId={taskId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("taskId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
                    PosterTaskDetailsScreen(
                        taskId = taskId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.PosterTasks.route) {
                    PosterTasksScreen(
                        onNavigateToPostTask = { navController.navigate(Screen.PosterPostTask.route) },
                        onNavigateToTaskDetails = { taskId ->
                            navController.navigate("poster/task_details?taskId=$taskId")
                        }
                    )
                }

                composable(Screen.PosterWallet.route) {
                    PosterWalletScreen(
                        onNavigateToAddMoney = { navController.navigate(Screen.PosterAddMoney.route) }
                    )
                }

                composable(Screen.PosterAddMoney.route) {
                    PosterAddMoneyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // --- Shared Screens ---
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onModeChanged = { newWorkerMode ->
                        isWorkerMode = newWorkerMode
                        val targetGraph = if (newWorkerMode) GRAPH_WORKER else GRAPH_POSTER
                        navController.navigate(targetGraph) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onSignOut = {
                        navController.navigate(Screen.WelcomeCarousel.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Sos.route) {
                SosScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
