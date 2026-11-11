package app.ranking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.ranking.ui.CompareScreen
import app.ranking.ui.ItemScreen
import app.ranking.ui.ListsScreen
import app.ranking.ui.RankingScreen
import app.ranking.ui.RankingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RankingTheme {
                AppNav()
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: RankingViewModel = viewModel()

    NavHost(navController = nav, startDestination = "lists") {
        composable("lists") {
            ListsScreen(vm = vm, onOpen = { listId -> nav.navigate("list/$listId") })
        }

        composable(
            route = "list/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { entry ->
            val listId = entry.arguments!!.getLong("listId")
            RankingScreen(
                vm = vm,
                listId = listId,
                onBack = { nav.popBackStack() },
                onOpenItem = { itemId -> nav.navigate("item/$itemId") },
                onCompare = { ids -> nav.navigate("compare/$listId/${ids.joinToString(",")}") }
            )
        }

        composable(
            route = "item/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { entry ->
            val itemId = entry.arguments!!.getLong("itemId")
            ItemScreen(vm = vm, itemId = itemId, onBack = { nav.popBackStack() })
        }

        composable(
            route = "compare/{listId}/{ids}",
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("ids") { type = NavType.StringType }
            )
        ) { entry ->
            val listId = entry.arguments!!.getLong("listId")
            val ids = entry.arguments!!.getString("ids").orEmpty()
            CompareScreen(vm = vm, listId = listId, idsParam = ids, onBack = { nav.popBackStack() })
        }
    }
}
