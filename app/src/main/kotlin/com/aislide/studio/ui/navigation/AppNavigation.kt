package com.aislide.studio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Single-activity navigation graph.
 *
 * Routes are kept as constants in [Routes] — never pass route strings inline.
 * Long-lived state is held in ViewModels scoped to the destination, with
 * SavedStateHandle for IDs (Rule 4 of android skill).
 */
object Routes {
    const val HOME = "home"
    const val AI_GENERATE = "ai_generate"
    const val EDITOR = "editor/{projectId}"
    const val SLIDE_EDITOR = "slide_editor/{projectId}/{slideIndex}"
    const val SETTINGS = "settings"
    const val EXPORT = "export/{projectId}"

    fun editor(projectId: Long) = "editor/$projectId"
    fun slideEditor(projectId: Long, slideIndex: Int) = "slide_editor/$projectId/$slideIndex"
    fun export(projectId: Long) = "export/$projectId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            // Placeholder — HomeScreen will be implemented next
            HomeScreenPlaceholder(navController)
        }
        composable(Routes.AI_GENERATE) {
            AiGenerateScreenPlaceholder(navController)
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            EditorScreenPlaceholder(navController, projectId)
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
            SlideEditorScreenPlaceholder(navController, projectId, slideIndex)
        }
        composable(Routes.SETTINGS) {
            SettingsScreenPlaceholder(navController)
        }
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            ExportScreenPlaceholder(navController, projectId)
        }
    }
}

// === Placeholders (will be replaced by real screens in next iteration) ===

@Composable
private fun HomeScreenPlaceholder(navController: androidx.navigation.NavHostController) {
    com.aislide.studio.ui.screens.home.HomeScreenRoute(navController)
}

@Composable
private fun AiGenerateScreenPlaceholder(navController: androidx.navigation.NavHostController) {
    com.aislide.studio.ui.screens.ai.AiGenerateScreenRoute(navController)
}

@Composable
private fun EditorScreenPlaceholder(
    navController: androidx.navigation.NavHostController,
    projectId: Long
) {
    com.aislide.studio.ui.screens.editor.EditorScreenRoute(navController, projectId)
}

@Composable
private fun SlideEditorScreenPlaceholder(
    navController: androidx.navigation.NavHostController,
    projectId: Long,
    slideIndex: Int
) {
    com.aislide.studio.ui.screens.slide.SlideEditorScreenRoute(navController, projectId, slideIndex)
}

@Composable
private fun SettingsScreenPlaceholder(navController: androidx.navigation.NavHostController) {
    com.aislide.studio.ui.screens.settings.SettingsScreenRoute(navController)
}

@Composable
private fun ExportScreenPlaceholder(
    navController: androidx.navigation.NavHostController,
    projectId: Long
) {
    com.aislide.studio.ui.screens.editor.ExportScreenRoute(navController, projectId)
}
