package com.hanziwriter.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hanziwriter.app.ui.home.HomeScreen
import com.hanziwriter.app.ui.calendar.CalendarScreen
import com.hanziwriter.app.ui.learn.DrillScreen
import com.hanziwriter.app.ui.learn.LearnScreen
import com.hanziwriter.app.ui.learn.QuizScreen
import com.hanziwriter.app.ui.setselector.SetSelectorScreen

// ═══════════════════════════════════════════════════════════════════
// Route definitions
// ═══════════════════════════════════════════════════════════════════
//
// Each constant defines a route pattern with {placeholders} for arguments.
// The helper functions below build concrete route strings that the
// NavController uses to navigate.
//
// ── Navigation glossary ──
//   NavController   : the "remote control" for navigation — it manages
//                      a back stack (like a browser history) of screens.
//   NavHost         : the Composable that hosts all destinations (screens).
//   composable()    : registers one screen at a given route pattern.
//   navArgument     : declares the name & type of a {placeholder} argument.
//   popBackStack()  : removes the current screen (goes back).
//   popUpTo(route)  : removes screens from the stack up to (and optionally
//                      including) the given route.
//
// ── How a screen transition works ──
//   1. A user taps a button → a callback fires.
//   2. That callback calls navController.navigate("some/route/value").
//   3. The NavHost matches "some/route/value" against its registered patterns.
//   4. The matching composable() block runs, parsing the {value} from the route.
//   5. The block calls a screen Composable (e.g. LearnScreen) with the parsed data.
// ═══════════════════════════════════════════════════════════════════

object Routes {

    // ── Screen 1: Set Selector  ──
    // The first screen the user sees — pick a character set to study.
    const val SET_SELECTOR = "set_selector"

    // ── Screen 2: Home (dashboard)  ──
    // Shows the chosen set name, streak info, and activity cards.
    // {setName} is the directory name of the selected character set (e.g. "hsk1").
    const val HOME = "home/{setName}"

    // ── Screens 3-5: Learn, Drill, Quiz  ──
    // Each accepts a comma-separated list of Unicode code points.
    // Example route: "learn/25105,22909,22823"  (three characters)
    // {unicodes} is stored as a plain String and parsed into a list of Ints.
    const val LEARN = "learn/{unicodes}"
    const val DRILL = "drill/{unicodes}"
    const val QUIZ = "quiz/{unicodes}"

    // ── Screen 6: Quiz Results  ──
    const val RESULTS = "results/{mode}/{score}"

    // ── Screen 7: Calendar  ──
    const val CALENDAR = "calendar"

    // ── Helper functions that build concrete route strings ──

    fun home(setName: String) = "home/$setName"

    /**
     * Builds a learn route from a list of Unicode code points.
     * Example: learn(listOf(25105, 22909, 22823)) → "learn/25105,22909,22823"
     */
    fun learn(unicodes: List<Int>) = "learn/${unicodes.joinToString(",")}"

    fun drill(unicodes: List<Int>) = "drill/${unicodes.joinToString(",")}"

    fun quiz(unicodes: List<Int>) = "quiz/${unicodes.joinToString(",")}"

    fun results(mode: String, score: Int) = "results/$mode/$score"
}

@Composable
fun NavGraph(
    // If the user previously selected a character set, skip the set selector
    // and go straight to the Home screen. Null means first launch.
    savedSetName: String? = null,
    // Callback invoked when the user picks a set — the caller (MainActivity)
    // should persist the name so it can be passed back as savedSetName next time.
    onSelectSet: (String) -> Unit = {}
) {
    // ── rememberNavController() ──
    // Creates a NavController that survives recomposition (screen rotations, etc.).
    // Think of it as the navigation "remote control":
    //   navController.navigate("route")  → push a new screen onto the stack
    //   navController.popBackStack()     → go back to the previous screen
    val navController = rememberNavController()

    // ── NavHost ──
    // The container that holds all destinations. The system matches the
    // current route against each composable() pattern and shows the matching one.
    // startDestination is the first screen displayed when the app opens.
    // If a set was previously saved, start directly at Home; otherwise show the
    // Set Selector for first-time setup.
    val startDestination = if (!savedSetName.isNullOrBlank()) {
        Routes.home(savedSetName)
    } else {
        Routes.SET_SELECTOR
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ══════════════════════════════════════════════
        // 1. Set Selector Screen
        // ══════════════════════════════════════════════
        composable(Routes.SET_SELECTOR) {
            SetSelectorScreen(
                onSetSelected = { setName ->
                    onSelectSet(setName)
                    navController.navigate(Routes.home(setName)) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // ══════════════════════════════════════════════
        // 2. Home Screen
        // ══════════════════════════════════════════════
        composable(
            route = Routes.HOME,
            arguments = listOf(navArgument("setName") { type = NavType.StringType })
        ) {
            // HomeViewModel gets "setName" automatically from SavedStateHandle
            HomeScreen(
                // Each callback now receives a List<Int> (the full set of unicodes
                // for that activity) instead of a single Int.
                onNavigateToLearn = { unicodes ->
                    navController.navigate(Routes.learn(unicodes))
                },
                onNavigateToDrill = { unicodes ->
                    navController.navigate(Routes.drill(unicodes))
                },
                onNavigateToQuiz = { unicodes ->
                    navController.navigate(Routes.quiz(unicodes))
                },
                onViewCalendar = {
                    navController.navigate(Routes.CALENDAR)
                },
                onChangeSet = {
                    navController.navigate(Routes.SET_SELECTOR)
                }
            )
        }

        // ══════════════════════════════════════════════
        // 3. Learn Screen
        // ══════════════════════════════════════════════
        composable(
            route = Routes.LEARN,
            // NavType.StringType because {unicodes} is a comma-separated string
            arguments = listOf(navArgument("unicodes") { type = NavType.StringType })
        ) { backStackEntry ->
            // Read the raw string from the route arguments ("25105,22909,22823")
            val unicodesStr = backStackEntry.arguments?.getString("unicodes") ?: ""
            // Split by comma and convert each piece to an Int (skipping invalid entries)
            val unicodes = unicodesStr.split(",").mapNotNull { it.toIntOrNull() }

            LearnScreen(
                unicodes = unicodes,              // ← now a list
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // ══════════════════════════════════════════════
        // 4. Drill Screen
        // ══════════════════════════════════════════════
        composable(
            route = Routes.DRILL,
            arguments = listOf(navArgument("unicodes") { type = NavType.StringType })
        ) { backStackEntry ->
            val unicodesStr = backStackEntry.arguments?.getString("unicodes") ?: ""
            val unicodes = unicodesStr.split(",").mapNotNull { it.toIntOrNull() }

            DrillScreen(
                unicodes = unicodes,
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // ══════════════════════════════════════════════
        // 5. Quiz Screen
        // ══════════════════════════════════════════════
        composable(
            route = Routes.QUIZ,
            arguments = listOf(navArgument("unicodes") { type = NavType.StringType })
        ) { backStackEntry ->
            val unicodesStr = backStackEntry.arguments?.getString("unicodes") ?: ""
            val unicodes = unicodesStr.split(",").mapNotNull { it.toIntOrNull() }

            QuizScreen(
                unicodes = unicodes,
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // ══════════════════════════════════════════════
        // 6. Calendar Screen
        // ══════════════════════════════════════════════
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
