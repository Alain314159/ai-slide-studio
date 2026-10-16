package com.aislide.studio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val HOME = "home"
    const val AI_GENERATE = "ai_generate"
    const val EDITOR = "editor/{projectId}"
    const val SLIDE_EDITOR = "slide_editor/{projectId}/{slideIndex}"
    const val SETTINGS = "settings"
    const val EXPORT = "export/{projectId}"
    const val PRESENTATION = "presentation/{projectId}?initial={initial}"

    fun editor(projectId: Long) = "editor/$projectId"
    fun slideEditor(projectId: Long, slideIndex: Int) = "slide_editor/$projectId/$slideIndex"
    fun export(projectId: Long) = "export/$projectId"
    fun presentation(projectId: Long, initial: Int = 0) = "presentation/$projectId?initial=$initial"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            com.aislide.studio.ui.screens.home.HomeScreenRoute(navController)
        }
        composable(Routes.AI_GENERATE) {
            com.aislide.studio.ui.screens.ai.AiGenerateScreenRoute(navController)
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            com.aislide.studio.ui.screens.editor.EditorScreenRoute(navController, projectId)
        }
        composable(
            route = Routes.SLIDE_EDITOR,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("slideIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val slideIndex = backStackEntry.arguments?.getInt("slideIndex") ?: 0
            com.aislide.studio.ui.screens.slide.SlideEditorScreenRoute(navController, projectId, slideIndex)
        }
        composable(Routes.SETTINGS) {
            com.aislide.studio.ui.screens.settings.SettingsScreenRoute(navController)
        }
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            com.aislide.studio.ui.screens.editor.ExportScreenRoute(navController, projectId)
        }
        composable(
            route = Routes.PRESENTATION,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("initial") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val initial = backStackEntry.arguments?.getInt("initial") ?: 0
            com.aislide.studio.ui.screens.presentation.PresentationScreenRoute(navController, initial)
        }
    }
}
