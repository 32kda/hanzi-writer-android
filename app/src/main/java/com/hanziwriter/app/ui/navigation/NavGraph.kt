package com.hanziwriter.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hanziwriter.app.ui.home.HomeScreen
import com.hanziwriter.app.ui.learn.LearnScreen
import com.hanziwriter.app.ui.setselector.SetSelectorScreen

object Routes {
    const val SET_SELECTOR = "set_selector"
    const val HOME = "home/{setName}"
    const val LEARN = "learn/{unicode}"
    const val DRILL = "drill/{unicode}"
    const val QUIZ = "quiz/{unicode}"
    const val RESULTS = "results/{mode}/{score}"

    fun home(setName: String) = "home/$setName"
    fun learn(unicode: Int) = "learn/$unicode"
    fun drill(unicode: Int) = "drill/$unicode"
    fun quiz(unicode: Int) = "quiz/$unicode"
    fun results(mode: String, score: Int) = "results/$mode/$score"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SET_SELECTOR) {
        composable(Routes.SET_SELECTOR) {
            SetSelectorScreen(
                onSetSelected = { setName ->
                    navController.navigate(Routes.home(setName)) {
                        popUpTo(Routes.SET_SELECTOR) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.HOME,
            arguments = listOf(navArgument("setName") { type = NavType.StringType })
        ) {
            HomeScreen(
                onNavigateToLearn = { unicode ->
                    navController.navigate(Routes.learn(unicode))
                },
                onNavigateToDrill = { unicode ->
                    navController.navigate(Routes.drill(unicode))
                },
                onNavigateToQuiz = { unicode ->
                    navController.navigate(Routes.quiz(unicode))
                },
                onChangeSet = {
                    navController.navigate(Routes.SET_SELECTOR) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.LEARN,
            arguments = listOf(navArgument("unicode") { type = NavType.IntType })
        ) { backStackEntry ->
            val unicode = backStackEntry.arguments?.getInt("unicode") ?: 0
            LearnScreen(
                unicode = unicode,
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DRILL,
            arguments = listOf(navArgument("unicode") { type = NavType.IntType })
        ) { backStackEntry ->
            val unicode = backStackEntry.arguments?.getInt("unicode") ?: 0
            LearnScreen(
                unicode = unicode,
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(navArgument("unicode") { type = NavType.IntType })
        ) { backStackEntry ->
            val unicode = backStackEntry.arguments?.getInt("unicode") ?: 0
            LearnScreen(
                unicode = unicode,
                onComplete = {
                    navController.navigate(Routes.results("quiz", 80))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULTS,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType }
            )
        ) {
            androidx.compose.material3.Text("Results screen")
        }
    }
}
