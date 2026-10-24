package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.AutoGraph
import com.maxrave.simpmusic.ui.icon.Home
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Sensors
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.MixForYouDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.StringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.analytics
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.home
import simpmusic.composeapp.generated.resources.mix
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.your_library
import kotlin.reflect.KClass

@Composable
expect fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean = false,
    showAnalyticsTab: Boolean = false,
    showMixForYouTab: Boolean = false,
    onOpenNowPlaying: () -> Unit = {},
    // False draws the same folding bar in the flat material (liquid glass turned off).
    liquidGlass: Boolean = true,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
)

/** Default 24 dp. The phone bar raises it to 28 dp; the glyph itself stays the same. */
val LocalBottomNavIconSize = compositionLocalOf { 24.dp }

sealed class BottomNavScreen(
    val ordinal: Int,
    val destination: Any,
    val title: StringResource,
    val icon: @Composable () -> Unit,
) {
    data object Home : BottomNavScreen(
        ordinal = 0,
        destination = HomeDestination,
        title = Res.string.home,
        icon = {
            Icon(
                SimpIcons.Home,
                contentDescription = null,
                modifier = Modifier.size(LocalBottomNavIconSize.current),
            )
        },
    )

    data object Search : BottomNavScreen(
        ordinal = 1,
        destination = SearchDestination,
        title = Res.string.search,
        icon = {
            Icon(
                SimpIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(LocalBottomNavIconSize.current),
            )
        },
    )

    data object Library : BottomNavScreen(
        ordinal = 2,
        destination = LibraryDestination,
        title = Res.string.your_library,
        icon = {
            Icon(
                imageVector = SimpIcons.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(LocalBottomNavIconSize.current),
            )
        },
    )

    /** Opens the Create sheet; not a navigation destination. */
    data object Create : BottomNavScreen(
        ordinal = 5,
        destination = LibraryDestination,
        title = Res.string.create,
        icon = {
            Icon(
                imageVector = SimpIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(LocalBottomNavIconSize.current),
            )
        },
    )

    // Only shown when local tracking is enabled.
    data object Analytics : BottomNavScreen(
        ordinal = 3,
        destination = AnalyticsDestination,
        title = Res.string.analytics,
        icon = {
            Icon(
                imageVector = SimpIcons.AutoGraph,
                contentDescription = null,
                modifier = Modifier.size(LocalBottomNavIconSize.current),
            )
        },
    )

    // Only shown while signed in to YouTube — an anonymous session gets no mixes.
    // Labelled "Mix", not "Mix for you": the full title is the widest label in the bar and forces
    // every tab to be that wide. The screen itself still uses the full title.
    data object MixForYou : BottomNavScreen(
        ordinal = 4,
        destination = MixForYouDestination,
        title = Res.string.mix,
        icon = {
            Icon(
                imageVector = SimpIcons.Sensors,
                contentDescription = null,
                modifier = Modifier.size(LocalBottomNavIconSize.current),
            )
        },
    )
}