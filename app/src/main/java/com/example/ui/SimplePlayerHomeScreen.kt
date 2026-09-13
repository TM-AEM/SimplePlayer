package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import coil.size.Scale
import com.example.R
import com.example.data.VideoItem
import com.example.utils.PermissionUtils

/**
 * Main Home Screen displaying the Device Video Library in a YouTube-style 2-column Grid.
 * Includes MediaStore querying, real-time live search, multiple sorting modes,
 * thumbnail generation with Coil, permissions flow, and empty states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePlayerHomeScreen(
    onPlayVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: VideoListViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle to re-check permission when returning from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Memoize Coil ImageLoader with VideoFrameDecoder, Memory Cache (max 25%), and Disk Cache (max 50MB)
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("video_thumbnails_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    val gridState = rememberLazyGridState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("simple_player_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.video_library_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("topbar_title")
                        )
                        if (uiState.hasPermission && uiState.allVideos.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    id = R.string.device_videos_count,
                                    uiState.displayedVideos.size
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.hasPermission) {
                        IconButton(
                            onClick = { viewModel.loadVideos() },
                            modifier = Modifier.testTag("refresh_library_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(id = R.string.refresh_library)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_app_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // 1. Permission not granted state
                !uiState.hasPermission -> {
                    PermissionRequestScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(PermissionUtils.requiredVideoPermission)
                        },
                        onOpenSettings = {
                            openAppSettings(context)
                        },
                        onPlaySample = {
                            onPlayVideo(sampleVideos.first())
                        }
                    )
                }

                // 2. Loading state
                uiState.isLoading && uiState.allVideos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("loading_indicator_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(44.dp))
                            Text(
                                text = "جارٍ فحص مكتبة الفيديوهات…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Main Grid layout with Search and Sorting
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("video_library_grid"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Search Bar Header (spans 2 columns)
                        item(span = { GridItemSpan(2) }) {
                            SearchBarHeader(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                onClear = { viewModel.setSearchQuery("") }
                            )
                        }

                        // Sorting Chips Row (spans 2 columns)
                        item(span = { GridItemSpan(2) }) {
                            SortingChipsRow(
                                currentSort = uiState.sortOrder,
                                onSortSelected = { viewModel.setSortOrder(it) }
                            )
                        }

                        // Empty Search State
                        if (uiState.displayedVideos.isEmpty() && uiState.searchQuery.isNotBlank()) {
                            item(span = { GridItemSpan(2) }) {
                                EmptySearchState(
                                    query = uiState.searchQuery,
                                    onClearSearch = { viewModel.setSearchQuery("") }
                                )
                            }
                        }
                        // Empty Device Storage State
                        else if (uiState.allVideos.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                EmptyStorageState(
                                    onRefresh = { viewModel.loadVideos() },
                                    onPlaySample = { onPlayVideo(sampleVideos.first()) }
                                )
                            }
                        }
                        // Video Items Grid
                        else {
                            items(
                                items = uiState.displayedVideos,
                                key = { item: VideoItem -> if (item.id != 0L) item.id else item.uri.toString() }
                            ) { video ->
                                VideoGridCard(
                                    video = video,
                                    imageLoader = imageLoader,
                                    onClick = { onPlayVideo(video) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * YouTube-style Video Card showing thumbnail with time badge and size badge,
 * followed by title and format details.
 */
@Composable
fun VideoGridCard(
    video: VideoItem,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(video.uri) {
        ImageRequest.Builder(context)
            .data(video.uri)
            .videoFrameMillis(1000L)
            .decoderFactory(VideoFrameDecoder.Factory())
            .size(width = 480, height = 270) // Downsample thumbnail dimensions to conserve memory
            .precision(Precision.INEXACT)
            .scale(Scale.FILL)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail container with 16:9 ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF252525)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )

                // Play icon overlay center
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Floating Duration Badge (Bottom-Right)
                if (video.durationMs > 0L) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Floating Size Badge (Bottom-Left)
                if (video.sizeBytes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Video Title & Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.testTag("video_title_${video.id}")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (video.durationMs > 0L) {
                        Text(
                            text = video.formattedDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Search Bar Header component for live filtering by filename.
 */
@Composable
fun SearchBarHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(text = stringResource(id = R.string.search_videos_placeholder))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(id = R.string.clear_search)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_text_field")
    )
}

/**
 * Sorting Chips row for selecting order (Date, Name, Size).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortingChipsRow(
    currentSort: VideoSortOrder,
    onSortSelected: (VideoSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Sort,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.sort_by),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = currentSort == VideoSortOrder.DATE_DESC,
                    onClick = { onSortSelected(VideoSortOrder.DATE_DESC) },
                    label = { Text(stringResource(id = R.string.sort_date_desc)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("sort_chip_date")
                )
            }
            item {
                FilterChip(
                    selected = currentSort == VideoSortOrder.NAME_ASC,
                    onClick = { onSortSelected(VideoSortOrder.NAME_ASC) },
                    label = { Text(stringResource(id = R.string.sort_name_asc)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("sort_chip_name")
                )
            }
            item {
                FilterChip(
                    selected = currentSort == VideoSortOrder.SIZE_DESC,
                    onClick = { onSortSelected(VideoSortOrder.SIZE_DESC) },
                    label = { Text(stringResource(id = R.string.sort_size_desc)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("sort_chip_size")
                )
            }
        }
    }
}

/**
 * Empty search results state.
 */
@Composable
fun EmptySearchState(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("empty_search_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = stringResource(id = R.string.no_search_results_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "لم يتم العثور على أي مقطع فيديو يطابق \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onClearSearch,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("reset_search_button")
            ) {
                Text(text = stringResource(id = R.string.clear_search))
            }
        }
    }
}

/**
 * Empty device storage state with quick sample play affordance.
 */
@Composable
fun EmptyStorageState(
    onRefresh: () -> Unit,
    onPlaySample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("empty_storage_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            )
            Text(
                text = stringResource(id = R.string.no_videos_found_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.no_videos_found_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlaySample,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_play_sample_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.play_sample_button))
                }
                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_refresh_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تحديث")
                }
            }
        }
    }
}

/**
 * Screen presented when storage / media permission is not granted.
 * Provides clear explanation, Grant Permission button, and Open Settings button.
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlaySample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("permission_request_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FolderShared,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.permission_required_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.permission_required_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Grant Button
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_permission_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.grant_permission_button),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Open App Settings Button
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.open_settings_button))
                }

                // Fallback sample test button
                Text(
                    text = "أو يمكنك اختبار المشغل مباشرة عبر عينة تجريبية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onPlaySample,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("sample_play_fallback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.play_sample_button))
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Fallback sample videos for testing and immediate playback
private val sampleVideos = listOf(
    VideoItem(
        id = 1L,
        title = "Big Buck Bunny (Sample)",
        uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        durationMs = 596000L,
        sizeBytes = 158000000L
    ),
    VideoItem(
        id = 2L,
        title = "For Bigger Blazes (Sample)",
        uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
        durationMs = 15000L,
        sizeBytes = 15000000L
    )
)
