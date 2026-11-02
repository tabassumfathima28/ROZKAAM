package com.rozkaam.app.ui.navigation

sealed class Screen(val route: String) {
    // Before Login / Onboarding
    object Splash : Screen("splash")
    object WelcomeCarousel : Screen("welcome_carousel")
    object Login : Screen("login")
    object RoleSelection : Screen("role_selection")

    // Worker Nested Graph
    object WorkerHome : Screen("worker/home")
    object WorkerTaskDetails : Screen("worker/task_details")
    object WorkerActiveJob : Screen("worker/active_job")
    object WorkerUploadProof : Screen("worker/upload_proof")
    object WorkerJobCompleted : Screen("worker/job_completed")
    object WorkerEarnings : Screen("worker/earnings")
    object WorkerWithdraw : Screen("worker/withdraw")
    object WorkerTasks : Screen("worker/tasks")

    // Poster Nested Graph
    object PosterHome : Screen("poster/home")
    object PosterPostTask : Screen("poster/post_task")
    object PosterTaskDetails : Screen("poster/task_details")
    object PosterTasks : Screen("poster/tasks")
    object PosterWallet : Screen("poster/wallet")
    object PosterAddMoney : Screen("poster/add_money")

    // Shared Screens
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    object Sos : Screen("sos")
}
