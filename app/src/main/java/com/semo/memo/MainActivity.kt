package com.semo.memo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.semo.memo.data.BundleWithMemos
import com.semo.memo.data.MemoEntity
import com.semo.memo.data.TimelineItem
import com.semo.memo.data.displayTitle
import com.semo.memo.data.previewText
import com.semo.memo.ui.AppAccent
import com.semo.memo.ui.AppBackground
import com.semo.memo.ui.AppBorder
import com.semo.memo.ui.AppSurface
import com.semo.memo.ui.AppSurfaceVariant
import com.semo.memo.ui.InactiveIcon
import com.semo.memo.ui.PrimaryText
import com.semo.memo.ui.SecondaryText
import com.semo.memo.ui.StrongText
import com.semo.memo.ui.TimeText
import com.semo.memo.ui.timelineIndexForEvent
import com.semo.memo.ui.SemoTheme
import com.semo.memo.ui.SemoViewModel
import com.semo.memo.ui.UiEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<SemoViewModel> {
        val app = application as SemoApplication
        object : AbstractSavedStateViewModelFactory(this, intent.extras) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T =
                SemoViewModel(app.repository, app.preferences, handle) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App UI is always light; keep dark system-bar icons even when the device is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent { SemoTheme { SemoApp(viewModel) } }
    }
}

private object Route {
    const val Timeline = "timeline"
    const val Bundles = "bundles"
    const val Settings = "settings"
    const val Search = "search"
    const val Detail = "bundle/{id}"
    fun detail(id: Long) = "bundle/$id"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SemoApp(vm: SemoViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val topLevel = route in setOf(Route.Timeline, Route.Bundles, Route.Settings)
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text)
                is UiEvent.BundleCreated -> snackbar.showSnackbar("${event.count}개의 메모를 묶었습니다.")
                is UiEvent.MemoCreated -> Unit
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (topLevel && !imeVisible) {
                BottomNav(route) { destination ->
                    nav.navigate(destination) {
                        popUpTo(Route.Timeline) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, Route.Timeline, Modifier.padding(padding)) {
            composable(Route.Timeline) {
                TimelineScreen(vm, onSearch = { nav.navigate(Route.Search) }, onBundle = { nav.navigate(Route.detail(it)) })
            }
            composable(Route.Bundles) { BundleListScreen(vm, onBundle = { nav.navigate(Route.detail(it)) }) }
            composable(Route.Settings) { SettingsScreen(vm) }
            composable(Route.Search) { SearchScreen(vm, onBack = nav::popBackStack, onBundle = { nav.navigate(Route.detail(it)) }) }
            composable(Route.Detail, arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                BundleDetailScreen(vm, backStack.arguments?.getLong("id") ?: 0, nav::popBackStack)
            }
        }
    }
}

@Composable
private fun BottomNav(current: String?, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppSurface).navigationBarsPadding()) {
        HorizontalDivider(color = AppBorder)
        Row(
            Modifier.fillMaxWidth().height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Triple(Route.Timeline, Icons.Default.Home, "메모"),
                Triple(Route.Bundles, Icons.Default.Folder, "정리"),
                Triple(Route.Settings, Icons.Default.Settings, "설정"),
            ).forEach { (route, icon, label) ->
                val selected = current == route
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onNavigate(route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, label, tint = if (selected) AppAccent else InactiveIcon, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(label, color = if (selected) AppAccent else InactiveIcon, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineScreen(vm: SemoViewModel, onSearch: () -> Unit, onBundle: (Long) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf(vm.draft) }
    var editing by remember { mutableStateOf<MemoEntity?>(null) }
    var deleting by remember { mutableStateOf<MemoEntity?>(null) }
    var actionMemo by remember { mutableStateOf<MemoEntity?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var initialScrollDone by rememberSaveable { mutableStateOf(false) }
    var composerHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.isLoading, state.timeline.size) {
        if (!state.isLoading && !initialScrollDone && state.timeline.isNotEmpty()) {
            listState.scrollToItem(state.timeline.lastIndex)
            initialScrollDone = true
        }
    }
    LaunchedEffect(vm, listState) {
        vm.events.filter { it is UiEvent.MemoCreated || it is UiEvent.BundleCreated }.collect { event ->
            val targetIndex = snapshotFlow { timelineIndexForEvent(state.timeline, event) }
                .filter { it >= 0 }
                .first()
            withFrameNanos { }
            listState.animateScrollToItem(targetIndex)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
                title = {
                    Text(
                        if (state.selectionMode) "${state.selectedMemoIds.size}개 선택" else "세모",
                        color = StrongText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    if (state.selectionMode) IconButton(vm::clearSelection) { Icon(Icons.Default.Close, "선택 취소") }
                },
                actions = {
                    if (state.selectionMode) {
                        Button(onClick = vm::bundleSelected, enabled = state.selectedMemoIds.isNotEmpty()) { Text("묶기") }
                    } else {
                        IconButton(onSearch) { Icon(Icons.Default.Search, "메모와 묶음 검색") }
                    }
                },
            )
        },
        bottomBar = {
            if (!state.selectionMode) MemoComposer(
                text = draft,
                onText = { draft = it; vm.draft = it },
                onSend = {
                    vm.sendMemo(draft) { draft = "" }
                },
                onHeightChanged = { height ->
                    if (composerHeight > 0 && height > composerHeight && state.timeline.isNotEmpty()) {
                        val layout = listState.layoutInfo
                        val nearLatest = layout.visibleItemsInfo.lastOrNull()?.index
                            ?.let { it >= layout.totalItemsCount - 2 } == true
                        if (nearLatest) scope.launch { listState.animateScrollToItem(state.timeline.lastIndex) }
                    }
                    composerHeight = height
                },
            )
        },
    ) { padding ->
        if (!state.isLoading && state.timeline.isEmpty()) {
            EmptyState("생각나는 것을 바로 적어보세요", "메모는 이 기기에 안전하게 저장됩니다.", Modifier.padding(padding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                reverseLayout = false,
            ) {
                items(state.timeline, key = { it.stableKey }) { item ->
                    when (item) {
                        is TimelineItem.DateSeparatorItem -> DateSeparator(item.label)
                        is TimelineItem.MemoItem -> MemoBubble(
                            item.memo,
                            selected = item.memo.id in state.selectedMemoIds,
                            showTime = item.showTime,
                            modifier = Modifier.padding(top = if (item.startsGroup) 10.dp else 3.dp),
                            onClick = {
                                if (state.selectionMode) vm.toggleSelection(item.memo.id)
                                else actionMemo = item.memo
                            },
                            onLongClick = { vm.toggleSelection(item.memo.id) },
                        )
                        is TimelineItem.BundleItem -> BundleCard(
                            item.value,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            onClick = { onBundle(item.value.bundle.id) },
                        )
                    }
                }
            }
        }
    }
    actionMemo?.let { memo ->
        MemoActionSheet(
            memo = memo,
            onDismiss = { actionMemo = null },
            onEdit = { actionMemo = null; editing = memo },
            onDelete = { actionMemo = null; deleting = memo },
        )
    }
    editing?.let { memo -> EditMemoDialog(memo, onDismiss = { editing = null }) { vm.updateMemo(memo.id, it); editing = null } }
    deleting?.let { memo -> ConfirmDialog(
        title = "메모를 삭제할까요?",
        body = "묶음에 포함된 메모라면 연결에서도 제거됩니다. 묶음의 편집 내용은 바뀌지 않습니다.",
        confirm = "삭제",
        onDismiss = { deleting = null },
    ) { vm.deleteMemo(memo.id); deleting = null } }
}

@Composable
internal fun MemoComposer(
    text: String,
    onText: (String) -> Unit,
    onSend: () -> Unit,
    onHeightChanged: (Int) -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().background(AppSurface).onSizeChanged { onHeightChanged(it.height) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.weight(1f)
                .heightIn(min = 48.dp, max = 136.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppSurface)
                .border(1.dp, AppBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText, lineHeight = 22.sp),
            cursorBrush = SolidColor(AppAccent),
            maxLines = 4,
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text("메모 입력", color = SecondaryText)
                    innerTextField()
                }
            },
        )
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier.size(48.dp).semantics { contentDescription = "메모 보내기" },
        ) {
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(if (text.isNotBlank()) AppAccent else AppSurface)
                    .border(1.dp, if (text.isNotBlank()) AppAccent else AppBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    null,
                    tint = if (text.isNotBlank()) Color.White else InactiveIcon,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MemoBubble(
    memo: MemoEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * .82f
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (showTime) {
                Text(
                    formatTime(memo.createdAt),
                    modifier = Modifier.testTag("memo-time-${memo.id}"),
                    color = TimeText,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(6.dp))
            }
            Box(
                Modifier.widthIn(max = maxBubbleWidth)
                    .testTag("memo-bubble-${memo.id}")
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (selected) Color(0xFFE3E5E8) else AppSurfaceVariant)
                    .then(if (selected) Modifier.border(1.dp, AppAccent, RoundedCornerShape(17.dp)) else Modifier)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(memo.content, color = PrimaryText, fontSize = 16.sp, lineHeight = 22.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoActionSheet(
    memo: MemoEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Text(
                memo.content,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Edit, null)
                Spacer(Modifier.width(10.dp))
                Text("수정", Modifier.weight(1f))
            }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Text("삭제", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("취소") }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = AppBorder)
        Text(label, color = SecondaryText, fontSize = 14.sp)
        HorizontalDivider(Modifier.weight(1f), color = AppBorder)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BundleCard(
    bundle: BundleWithMemos,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Box(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Card(
                modifier = Modifier.widthIn(min = 140.dp, max = 190.dp).clickable(onClick = onClick),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = BorderStroke(1.dp, AppBorder),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        bundle.displayTitle(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("원본 메모 ${bundle.memos.size}개", color = SecondaryText, fontSize = 12.sp)
                        if (bundle.bundle.isPinned) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.PushPin, "고정됨", tint = AppAccent, modifier = Modifier.size(15.dp))
                        }
                    }
                    HorizontalDivider(color = AppBorder)
                    Text(
                        bundle.previewText(),
                        color = SecondaryText,
                        fontSize = 14.sp,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                    )
                    Text(formatTime(bundle.bundle.createdAt), color = TimeText, fontSize = 11.sp)
                }
            }
        }
    }
}

private enum class BundleFilter { ALL, PINNED, ARCHIVED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BundleListScreen(vm: SemoViewModel, onBundle: (Long) -> Unit) {
    val bundles by vm.allBundles.collectAsStateWithLifecycle()
    val compact by vm.compactCards.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(BundleFilter.ALL) }
    val visible = bundles.filter { value ->
        val matchesFilter = when (filter) {
            BundleFilter.ALL -> !value.bundle.isArchived
            BundleFilter.PINNED -> value.bundle.isPinned && !value.bundle.isArchived
            BundleFilter.ARCHIVED -> value.bundle.isArchived
        }
        matchesFilter && (query.isBlank() || value.bundle.title.orEmpty().contains(query, true) ||
            value.bundle.editableContent.contains(query, true) || value.memos.any { it.content.contains(query, true) })
    }.sortedWith(compareByDescending<BundleWithMemos> { it.bundle.isPinned }.thenByDescending { it.bundle.updatedAt })

    Scaffold(
        containerColor = AppBackground,
        topBar = { CenterAlignedTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
            title = { Text("정리", fontWeight = FontWeight.Bold) },
        ) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("제목, 편집 내용, 원본 메모 검색") },
                singleLine = true,
            )
            Row(Modifier.padding(top = 8.dp, bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(BundleFilter.ALL to "전체", BundleFilter.PINNED to "고정", BundleFilter.ARCHIVED to "보관").forEach { (value, label) ->
                    if (filter == value) Button(
                        onClick = { filter = value },
                        modifier = Modifier.height(40.dp),
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) { Text(label) }
                    else OutlinedButton(
                        onClick = { filter = value },
                        modifier = Modifier.height(40.dp),
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) { Text(label) }
                }
            }
            if (visible.isEmpty()) EmptyState("표시할 묶음이 없습니다", "메모 화면에서 원본 메모를 길게 눌러 묶어보세요.", Modifier.weight(1f))
            else BundleGrid(visible, compact, onBundle, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun BundleGrid(
    bundles: List<BundleWithMemos>,
    compact: Boolean,
    onBundle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        gridItems(bundles, key = { it.bundle.id }) { bundle ->
            BundleGridCard(bundle, compact) { onBundle(bundle.bundle.id) }
        }
    }
}

@Composable
internal fun BundleGridCard(bundle: BundleWithMemos, compact: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(136.dp)
            .testTag("bundle-grid-${bundle.bundle.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text(
                bundle.displayTitle(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("원본 ${bundle.memos.size}개", color = SecondaryText, fontSize = 12.sp, maxLines = 1)
                if (bundle.bundle.isPinned) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.PushPin, "고정됨", tint = AppAccent, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                bundle.previewText(),
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(formatTime(bundle.bundle.createdAt), color = TimeText, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(vm: SemoViewModel, onBack: () -> Unit, onBundle: (Long) -> Unit) {
    val memos by vm.allMemos.collectAsStateWithLifecycle()
    val bundles by vm.allBundles.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val requester = remember { FocusRequester() }
    val memoResults = remember(query, memos) { if (query.isBlank()) emptyList() else memos.filter { !it.isDeleted && it.content.contains(query, true) } }
    val bundleResults = remember(query, bundles) { if (query.isBlank()) emptyList() else bundles.filter {
        it.bundle.title.orEmpty().contains(query, true) || it.bundle.editableContent.contains(query, true) || it.memos.any { memo -> memo.content.contains(query, true) }
    } }
    LaunchedEffect(Unit) { requester.requestFocus() }
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기") } },
                title = { Text("검색") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().focusRequester(requester),
                placeholder = { Text("메모와 묶음 검색") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (query.isBlank()) item { EmptyState("찾을 내용을 입력하세요", "원본 메모, 묶음 제목과 편집 내용을 검색합니다.") }
                else if (memoResults.isEmpty() && bundleResults.isEmpty()) item { EmptyState("검색 결과가 없습니다", "다른 검색어를 입력해보세요.") }
                if (memoResults.isNotEmpty()) item { SectionLabel("원본 메모 ${memoResults.size}") }
                items(memoResults, key = { "search-memo-${it.id}" }) { SearchMemoRow(it) }
                if (bundleResults.isNotEmpty()) item { SectionLabel("묶음 ${bundleResults.size}") }
                items(bundleResults, key = { "search-bundle-${it.bundle.id}" }) { BundleCard(it) { onBundle(it.bundle.id) } }
            }
        }
    }
}

@Composable
private fun SearchMemoRow(memo: MemoEntity) {
    Card(colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(memo.content, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(formatDateTime(memo.createdAt), color = SecondaryText, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BundleDetailScreen(vm: SemoViewModel, id: Long, onBack: () -> Unit) {
    val bundleFlow = remember(id) { vm.bundle(id) }
    val bundle by bundleFlow.collectAsStateWithLifecycle(initialValue = null)
    val allMemos by vm.allMemos.collectAsStateWithLifecycle()
    var title by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var content by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val value = bundle
    val latestTitle = rememberUpdatedState(title)
    val latestContent = rememberUpdatedState(content)

    LaunchedEffect(value?.bundle?.id) {
        val loaded = value ?: return@LaunchedEffect
        if (title == null) title = loaded.bundle.title.orEmpty()
        if (content == null) content = loaded.bundle.editableContent
    }

    DisposableEffect(id) {
        onDispose {
            val pendingTitle = latestTitle.value
            if (pendingTitle != null) vm.flushBundleTitle(id, pendingTitle)
            val pendingContent = latestContent.value
            if (pendingContent != null) vm.flushBundleContent(id, pendingContent)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기") } },
                title = { Text(value?.displayTitle() ?: "묶음", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "묶음 메뉴") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text(if (value?.bundle?.isPinned == true) "고정 해제" else "상단 고정") }, onClick = { menu = false; vm.togglePinned(id) }, leadingIcon = { Icon(Icons.Default.PushPin, null) })
                            DropdownMenuItem({ Text(if (value?.bundle?.isArchived == true) "보관 해제" else "보관") }, onClick = { menu = false; vm.toggleArchived(id) }, leadingIcon = { Icon(Icons.Default.Archive, null) })
                            DropdownMenuItem({ Text("삭제") }, onClick = { menu = false; deleting = true }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (value == null) EmptyState("묶음을 찾을 수 없습니다", "삭제되었거나 존재하지 않는 묶음입니다.", Modifier.padding(padding))
        else LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(
                    value = title.orEmpty(),
                    onValueChange = { title = it; vm.updateBundleTitleDebounced(id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("제목 (선택)") },
                    placeholder = { Text(value.displayTitle()) },
                    singleLine = true,
                )
            }
            item {
                SectionLabel("정리한 내용")
                OutlinedTextField(
                    value = content.orEmpty(),
                    onValueChange = { content = it; vm.updateBundleContentDebounced(id, it) },
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    placeholder = { Text("내용을 자유롭게 정리하세요") },
                )
                Text("입력은 자동 저장되며 원본 메모는 바뀌지 않습니다.", color = SecondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("포함된 원본 메모 ${value.memos.size}개", Modifier.weight(1f))
                    TextButton({ adding = true }) { Icon(Icons.Default.Add, null); Text("메모 추가") }
                }
            }
            items(value.memos.sortedBy { it.createdAt }, key = { it.id }) { memo ->
                Card(colors = CardDefaults.cardColors(containerColor = AppSurface), border = CardDefaults.outlinedCardBorder()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(memo.content)
                            Text(formatDateTime(memo.createdAt), color = SecondaryText, fontSize = 11.sp)
                        }
                        IconButton({ vm.removeMemo(id, memo.id) }) { Icon(Icons.Default.Close, "묶음에서 제거") }
                    }
                }
            }
        }
    }
    if (adding && value != null) {
        AddMemoDialog(
            memos = allMemos.filter { !it.isDeleted && it.id !in value.memos.map(MemoEntity::id).toSet() },
            onDismiss = { adding = false },
        ) { vm.addMemos(id, it); adding = false }
    }
    if (deleting) ConfirmDialog(
        "묶음을 삭제할까요?",
        "묶음만 삭제되며 포함된 원본 메모는 그대로 유지됩니다.",
        "삭제",
        { deleting = false },
    ) { deleting = false; vm.deleteBundle(id, onBack) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: SemoViewModel) {
    val compact by vm.compactCards.collectAsStateWithLifecycle()
    var clearing by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = AppBackground,
        topBar = { CenterAlignedTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
            title = { Text("설정", fontWeight = FontWeight.Bold) },
        ) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionLabel("화면") }
            item { SettingCard(Icons.Default.Info, "밝은 테마", "차분한 화이트와 무채색", trailing = { Text("기본") }) }
            item { SettingCard(Icons.Default.Folder, "간결한 묶음 카드", "묶음 목록을 더 촘촘하게 표시", trailing = { Switch(compact, { vm.setCompactCards(it) }) }) }
            item { SectionLabel("데이터") }
            item { SettingCard(Icons.Default.Archive, "데이터 내보내기·가져오기", "2차 단계에서 파일 백업으로 제공", trailing = { Text("준비 중", color = SecondaryText) }) }
            item { SettingCard(Icons.Default.Delete, "전체 데이터 삭제", "모든 메모와 묶음을 영구 삭제", onClick = { clearing = true }) }
            item { SectionLabel("앱 정보") }
            item { SettingCard(Icons.Default.Info, "세모", "채팅형 개인 메모 · 오프라인 전용", trailing = { Text("1.0.0") }) }
        }
    }
    if (clearing) ConfirmDialog(
        "모든 데이터를 삭제할까요?",
        "이 작업은 되돌릴 수 없습니다. 모든 원본 메모와 묶음이 영구적으로 삭제됩니다.",
        "전체 삭제",
        { clearing = false },
    ) { vm.clearAll(); clearing = false }
}

@Composable
private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = AppAccent)
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(body, color = SecondaryText, fontSize = 12.sp) }
            trailing?.invoke()
        }
    }
}

@Composable
private fun AddMemoDialog(memos: List<MemoEntity>, onDismiss: () -> Unit, onConfirm: (Set<Long>) -> Unit) {
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("원본 메모 추가") },
        text = {
            if (memos.isEmpty()) Text("추가할 수 있는 메모가 없습니다.")
            else LazyColumn(Modifier.fillMaxHeight(.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memos, key = { it.id }) { memo ->
                    Card(
                        onClick = { selected = selected.toMutableSet().apply { if (!add(memo.id)) remove(memo.id) } },
                        colors = CardDefaults.cardColors(containerColor = if (memo.id in selected) Color(0xFFE3E5E8) else AppSurfaceVariant),
                        border = if (memo.id in selected) BorderStroke(1.dp, AppAccent) else null,
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(memo.content, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (memo.id in selected) Icon(Icons.Default.CheckCircle, "선택됨", tint = AppAccent)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(selected) }, enabled = selected.isNotEmpty()) { Text("추가") } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}

@Composable
private fun EditMemoDialog(memo: MemoEntity, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember(memo.id) { mutableStateOf(memo.content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메모 수정") },
        text = { OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 3, maxLines = 8) },
        confirmButton = { TextButton({ onSave(text) }, enabled = text.isNotBlank()) { Text("저장") } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}

@Composable
private fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = SecondaryText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) { Text(text, modifier, color = SecondaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }

private val timeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd a h:mm", Locale.KOREAN)
private fun formatTime(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(timeFormatter)
private fun formatDateTime(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
