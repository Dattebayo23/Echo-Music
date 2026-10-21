package echo.music.iad1tya.ui.screens.playlist

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.spotify.models.SpotifyTrack
import echo.music.iad1tya.utils.makeTimeString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistDetailScreen(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
  playlistId: String,
  playlistTitle: String,
  playlistCover: String,
  viewModel: SpotifyPlaylistDetailViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val playerConnection = LocalPlayerConnection.current
  val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
    ?: remember { androidx.compose.runtime.mutableStateOf(null) }
  val isPlaying by playerConnection?.isEffectivelyPlaying?.collectAsState()
    ?: remember { androidx.compose.runtime.mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = uiState.title.ifBlank { playlistTitle },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(
              painter = painterResource(R.drawable.arrow_back),
              contentDescription = "Back",
            )
          }
        },
        actions = {
          IconButton(onClick = { viewModel.refresh() }) {
            Icon(
              painter = painterResource(R.drawable.cached),
              contentDescription = "Refresh",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
        ),
        scrollBehavior = scrollBehavior,
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = LocalPlayerAwareWindowInsets.current,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (uiState.isLoading && uiState.tracks.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 80.dp),
        ) {
          // Playlist Header Hero Card
          item(key = "header") {
            SpotifyPlaylistHeaderCard(
              title = uiState.title.ifBlank { playlistTitle },
              coverUrl = uiState.coverUrl ?: playlistCover,
              trackCount = if (uiState.totalCount > 0) uiState.totalCount else uiState.tracks.size,
              onPlayAll = {
                uiState.tracks.firstOrNull()?.let { firstTrack ->
                  viewModel.playTrack(
                    track = firstTrack,
                    onSuccess = { songItem ->
                      playerConnection?.playQueue(
                        YouTubeQueue(
                          endpoint = WatchEndpoint(videoId = songItem.id),
                          preloadItem = songItem.toMediaMetadata(),
                        )
                      )
                    },
                    onError = {
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Unable to match stream on YouTube/Addon")
                      }
                    }
                  )
                }
              },
              onShuffle = {
                val shuffled = uiState.tracks.shuffled()
                shuffled.firstOrNull()?.let { firstTrack ->
                  viewModel.playTrack(
                    track = firstTrack,
                    onSuccess = { songItem ->
                      playerConnection?.playQueue(
                        YouTubeQueue(
                          endpoint = WatchEndpoint(videoId = songItem.id),
                          preloadItem = songItem.toMediaMetadata(),
                        )
                      )
                    },
                    onError = {
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Unable to match stream on YouTube/Addon")
                      }
                    }
                  )
                }
              }
            )
          }

          // Track Rows
          itemsIndexed(
            items = uiState.tracks,
            key = { index, track -> "${track.id}_$index" }
          ) { index, track ->
            val isCurrentSong = mediaMetadata?.title.equals(track.name, ignoreCase = true)
            val isResolving = uiState.resolvingTrackId == track.id

            SpotifyTrackRow(
              index = index + 1,
              track = track,
              isActive = isCurrentSong,
              isPlaying = isCurrentSong && isPlaying,
              isResolving = isResolving,
              onClick = {
                if (isCurrentSong) {
                  playerConnection?.togglePlayPause()
                } else {
                  viewModel.playTrack(
                    track = track,
                    onSuccess = { songItem ->
                      playerConnection?.playQueue(
                        YouTubeQueue(
                          endpoint = WatchEndpoint(videoId = songItem.id),
                          preloadItem = songItem.toMediaMetadata(),
                        )
                      )
                    },
                    onError = {
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Could not match \"${track.name}\" to a playable stream")
                      }
                    }
                  )
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SpotifyPlaylistHeaderCard(
  title: String,
  coverUrl: String?,
  trackCount: Int,
  onPlayAll: () -> Unit,
  onShuffle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      modifier = Modifier.size(170.dp),
    ) {
      AsyncImage(
        model = coverUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "$trackCount tracks • Live Spotify Stream",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(
        onClick = onPlayAll,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.onSurface,
          contentColor = MaterialTheme.colorScheme.surface,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
      ) {
        Icon(
          painter = painterResource(R.drawable.play),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Play", fontWeight = FontWeight.Bold)
      }

      Button(
        onClick = onShuffle,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
      ) {
        Icon(
          painter = painterResource(R.drawable.shuffle),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Shuffle", fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}

@Composable
private fun SpotifyTrackRow(
  index: Int,
  track: SpotifyTrack,
  isActive: Boolean,
  isPlaying: Boolean,
  isResolving: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val coverUrl = SpotifyMapper.getTrackThumbnail(track)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .background(
        if (isActive) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        else Color.Transparent
      )
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Track index or indicator
    Box(
      modifier = Modifier.width(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isResolving) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.primary,
        )
      } else if (isActive) {
        Icon(
          painter = painterResource(
            if (isPlaying) R.drawable.volume_up else R.drawable.pause
          ),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      } else {
        Text(
          text = index.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    AsyncImage(
      model = coverUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(46.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = track.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      val artistText = track.artists.joinToString(", ") { it.name }
      Text(
        text = artistText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Text(
      text = makeTimeString(track.durationMs.toLong()),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    )
  }
}
