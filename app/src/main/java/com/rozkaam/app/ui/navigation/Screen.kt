package com.rozkaam.app.ui.navigation

sealed class Screen(val route: String) {
    // Before Login / Onboarding
    object Splash : Screen("splash")
    object WelcomeCarousel : Screen("welcome_carousel")
    object Login : Screen("login")
    object RoleSelection : Screen("role_selection")

    // Worker Nested Graph
    object WorkerHome : Screen("worker/home")
    object WorkerTaskDetails : Screen("worker/task_details") {
        fun createRoute(taskId: String = ""): String = if (taskId.isNotBlank()) "worker/task_details?taskId=$taskId" else route
    }
    object WorkerActiveJob : Screen("worker/active_job") {
        fun createRoute(taskId: String): String = "worker/active_job?taskId=$taskId"
    }
    object WorkerUploadProof : Screen("worker/upload_proof") {
        fun createRoute(taskId: String): String = "worker/upload_proof?taskId=$taskId"
    }
    object WorkerProofQrScanner : Screen("worker/proof_qr_scanner") {
        fun createRoute(taskId: String): String = "worker/proof_qr_scanner?taskId=$taskId"
    }
    object WorkerJobCompleted : Screen("worker/job_completed")
    object WorkerEarnings : Screen("worker/earnings")
    object WorkerWithdraw : Screen("worker/withdraw")
    object WorkerTasks : Screen("worker/tasks")

    // Poster Nested Graph
    object PosterHome : Screen("poster/home")
    object PosterPostTask : Screen("poster/post_task")
    object PosterTaskDetails : Screen("poster/task_details") {
        fun createRoute(taskId: String = ""): String = if (taskId.isNotBlank()) "poster/task_details?taskId=$taskId" else route
    }
    object PosterTasks : Screen("poster/tasks")
    object PosterWallet : Screen("poster/wallet")
    object PosterAddMoney : Screen("poster/add_money")

    // Shared Screens
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    object Sos : Screen("sos")
}
