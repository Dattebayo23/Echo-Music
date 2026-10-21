package echo.music.iad1tya.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.spotify.models.SpotifyPlaylist

private const val SPOTIFY_LIKED_SONGS_COVER = "https://misc.scdn.co/liked-songs/liked-songs-640.png"

@Composable
fun LibrarySpotifyScreen(
  navController: NavController,
  filterContent: @Composable () -> Unit,
  viewModel: LibrarySpotifyViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
      item(key = "filter") {
        filterContent()
      }

      if (uiState.isLoading && uiState.playlists.isEmpty()) {
        item(key = "loading") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(36.dp),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      } else if (!uiState.isAuthenticated) {
        item(key = "connect_card") {
          SpotifyConnectBanner(
            onConnect = {
              navController.navigate("settings/spotify_import")
            },
            onRefresh = {
              viewModel.refresh()
            }
          )
        }
      } else {
        // Authenticated Spotify Profile Header
        item(key = "profile_header") {
          SpotifyProfileHeader(
            accountName = uiState.accountName ?: "Spotify User",
            avatarUrl = uiState.avatarUrl,
            onRefresh = { viewModel.refresh() }
          )
        }

        // Hero Card: Liked Songs
        item(key = "liked_songs") {
          SpotifyHeroLikedCard(
            trackCount = uiState.likedSongsCount,
            onClick = {
              val titleEnc = Uri.encode("Liked Songs")
              val coverEnc = Uri.encode(SPOTIFY_LIKED_SONGS_COVER)
              navController.navigate("spotify_playlist/liked?title=$titleEnc&cover=$coverEnc")
            }
          )
        }

        // Header: Your Playlists
        item(key = "playlists_header") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.spotify_playlists_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "${uiState.playlists.size} playlists",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (uiState.playlists.isEmpty()) {
          item(key = "empty_playlists") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = stringResource(R.string.spotify_no_playlists_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          items(
            items = uiState.playlists,
            key = { it.id }
          ) { playlist ->
            val coverUrl = SpotifyMapper.getPlaylistThumbnail(playlist)
            SpotifyPlaylistRow(
              playlist = playlist,
              coverUrl = coverUrl,
              onClick = {
                val titleEnc = Uri.encode(playlist.name)
                val coverEnc = Uri.encode(coverUrl.orEmpty())
                navController.navigate("spotify_playlist/${playlist.id}?title=$titleEnc&cover=$coverEnc")
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SpotifyConnectBanner(
  onConnect: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(Color(0xFF1DB954).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_spotify),
          contentDescription = null,
          tint = Color(0xFF1DB954),
          modifier = Modifier.size(36.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.spotify_live_library),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.spotify_live_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onConnect,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.onSurface,
          contentColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(0.85f),
      ) {
        Text(
          text = stringResource(R.string.spotify_connect_account),
          fontWeight = FontWeight.SemiBold,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      TextButton(
        onClick = onRefresh,
      ) {
        Icon(
          painter = painterResource(R.drawable.cached),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Already connected? Tap to refresh",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun SpotifyProfileHeader(
  accountName: String,
  avatarUrl: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
          model = avatarUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        )
      } else {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(R.drawable.person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(
          text = accountName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(Color(0xFF1DB954))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Spotify Connected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    IconButton(onClick = onRefresh) {
      Icon(
        painter = painterResource(R.drawable.cached),
        contentDescription = "Refresh",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SpotifyHeroLikedCard(
  trackCount: Int?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AsyncImage(
        model = SPOTIFY_LIKED_SONGS_COVER,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(12.dp)),
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.spotify_liked_songs),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (trackCount != null) "$trackCount tracks" else stringResource(R.string.spotify_liked_songs_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Icon(
        painter = painterResource(R.drawable.navigate_next),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SpotifyPlaylistRow(
  playlist: SpotifyPlaylist,
  coverUrl: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AsyncImage(
      model = coverUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(54.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = playlist.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      val subtitle = buildString {
        playlist.owner?.displayName?.takeIf { it.isNotBlank() }?.let {
          append(it)
        }
        playlist.tracks?.total?.let { count ->
          if (isNotEmpty()) append(" • ")
          append("$count tracks")
        }
      }

      if (subtitle.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Icon(
      painter = painterResource(R.drawable.navigate_next),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )
  }
}
