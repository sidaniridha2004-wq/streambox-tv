package com.streambox.tv.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.GhostButton
import com.streambox.tv.ui.components.PosterCard
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.components.SecondaryButton
import com.streambox.tv.ui.components.SectionHeader
import com.streambox.tv.ui.components.StatusPill
import com.streambox.tv.ui.theme.Amber500
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun MovieDetailsScreen(nav: NavHostController, id: String, vm: MoviesViewModel = hiltViewModel()) {
    val movie = vm.get(id) ?: return
    val related = vm.related(id)

    Column(
        modifier = Modifier.fillMaxSize().background(Bg900).verticalScroll(rememberScrollState()),
    ) {
        // Backdrop area with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF11262F), Bg900))),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .aspectRatio(2f / 3f)
                        .background(Bg700, RoundedCornerShape(14.dp))
                        .border(1.dp, GlassStroke, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(movie.title.take(1).uppercase(), color = Teal400, style = MaterialTheme.typography.displayMedium) }

                Spacer(Modifier.width(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("MOVIE", color = Teal400)
                        StatusPill("★ ${movie.rating}", color = Amber500)
                        Text("${movie.year} · ${movie.genre} · ${movie.durationMin} min", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(movie.title, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(movie.description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton("Play", { nav.navigate(Routes.player(movie.streamUrl)) }, leadingIcon = Icons.Default.PlayArrow)
                        Spacer(Modifier.width(12.dp))
                        SecondaryButton(
                            text = if (movie.isFavorite) "In favorites" else "Add to favorites",
                            leadingIcon = if (movie.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            onClick = { vm.toggleFavorite(id) },
                        )
                        Spacer(Modifier.width(12.dp))
                        GhostButton("Stream info", onClick = {})
                    }
                }
            }
        }

        SectionHeader("More like this")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            items(related, key = { it.id }) { r ->
                Box(modifier = Modifier.width(140.dp)) {
                    PosterCard(
                        title = r.title,
                        subtitle = "${r.year} · ${r.genre}",
                        imageUrl = r.posterUrl,
                        onClick = { nav.navigate(Routes.movieDetails(r.id)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
