package com.semo.memo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semo.memo.data.BundleWithMemos
import com.semo.memo.data.MemoEntity
import com.semo.memo.data.SemoRepository
import com.semo.memo.data.TimelineItem
import com.semo.memo.data.UserPreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SemoUiState(
    val memos: List<MemoEntity> = emptyList(),
    val bundles: List<BundleWithMemos> = emptyList(),
    val timeline: List<TimelineItem> = emptyList(),
    val isLoading: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedMemoIds: Set<Long> = emptySet(),
)

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class BundleCreated(val count: Int, val id: Long) : UiEvent
    data class MemoCreated(val id: Long) : UiEvent
}

internal fun timelineIndexForEvent(timeline: List<TimelineItem>, event: UiEvent): Int = when (event) {
    is UiEvent.MemoCreated -> timeline.indexOfFirst { item ->
        item is TimelineItem.MemoItem && item.memo.id == event.id
    }
    is UiEvent.BundleCreated -> timeline.indexOfFirst { item ->
        item is TimelineItem.BundleItem && item.value.bundle.id == event.id
    }
    is UiEvent.Message -> -1
}

class SemoViewModel(
    private val repository: SemoRepository,
    private val preferences: UserPreferences,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val selection = MutableStateFlow(readSelection())
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val draft: StateFlow<String> = savedStateHandle.getStateFlow(KEY_DRAFT, "")

    fun updateDraft(value: String) {
        savedStateHandle[KEY_DRAFT] = value
    }

    val state: StateFlow<SemoUiState> = combine(
        repository.activeMemos,
        repository.activeBundles,
        selection,
    ) { memos, bundles, selected ->
        SemoUiState(
            memos = memos,
            bundles = bundles,
            timeline = buildTimeline(memos, bundles),
            isLoading = false,
            selectionMode = selected.isNotEmpty(),
            selectedMemoIds = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemoUiState())

    val allMemos = repository.allMemos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deletedMemos = repository.deletedMemos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allBundles = repository.allBundles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val compactCards = preferences.compactCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val recentSearches = preferences.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addRecentSearch(query: String) = action("검색어를 저장하지 못했습니다.") { preferences.addRecentSearch(query) }
    fun removeRecentSearch(query: String) = action("검색어를 지우지 못했습니다.") { preferences.removeRecentSearch(query) }

    fun bundle(id: Long) = repository.bundle(id)

    fun sendMemo(text: String, onSuccess: () -> Unit = {}) = action("메모를 저장하지 못했습니다.") {
        val id = repository.createMemo(text)
        updateDraft("")
        onSuccess()
        _events.emit(UiEvent.MemoCreated(id))
    }

    fun updateMemo(id: Long, text: String) = action("메모를 수정하지 못했습니다.") {
        repository.updateMemo(id, text)
        _events.emit(UiEvent.Message("메모를 수정했습니다."))
    }

    fun deleteMemo(id: Long) = action("메모를 삭제하지 못했습니다.") {
        val wasLinked = repository.deleteMemo(id)
        _events.emit(UiEvent.Message(if (wasLinked) "묶음 연결과 원본 메모를 휴지통으로 옮겼습니다." else "메모를 휴지통으로 옮겼습니다."))
    }

    fun restoreMemo(id: Long) = action("메모를 복원하지 못했습니다.") {
        repository.restoreMemo(id)
        _events.emit(UiEvent.Message("메모를 복원했습니다. 이전 묶음 연결은 되살아나지 않습니다."))
    }

    fun permanentlyDeleteMemo(id: Long) = action("메모를 영구 삭제하지 못했습니다.") {
        repository.permanentlyDeleteMemo(id)
        _events.emit(UiEvent.Message("메모를 영구 삭제했습니다."))
    }

    fun toggleSelection(id: Long) {
        persistSelection(
            selection.value.toMutableSet().apply {
                if (!add(id)) remove(id)
            },
        )
    }

    fun clearSelection() {
        persistSelection(emptySet())
    }

    fun bundleSelected() = action("묶음을 만들지 못했습니다.") {
        val ids = selection.value
        if (ids.isEmpty()) return@action
        val id = repository.createBundle(ids)
        persistSelection(emptySet())
        _events.emit(UiEvent.BundleCreated(ids.size, id))
    }

    private val titleSaveJobs = ConcurrentHashMap<Long, Job>()
    private val contentSaveJobs = ConcurrentHashMap<Long, Job>()

    fun updateBundleTitleDebounced(id: Long, title: String) {
        titleSaveJobs[id]?.cancel()
        val job = viewModelScope.launch {
            delay(450)
            runCatching { repository.updateBundleTitle(id, title) }
                .onFailure { _events.emit(UiEvent.Message("제목을 저장하지 못했습니다.")) }
        }
        job.invokeOnCompletion { titleSaveJobs.remove(id, job) }
        titleSaveJobs[id] = job
    }

    fun flushBundleTitle(id: Long, title: String) {
        titleSaveJobs.remove(id)?.cancel()
        viewModelScope.launch {
            runCatching { repository.updateBundleTitle(id, title) }
                .onFailure { _events.emit(UiEvent.Message("제목을 저장하지 못했습니다.")) }
        }
    }

    fun updateBundleContentDebounced(id: Long, content: String) {
        contentSaveJobs[id]?.cancel()
        val job = viewModelScope.launch {
            delay(450)
            runCatching { repository.updateBundleContent(id, content) }
                .onFailure { _events.emit(UiEvent.Message("편집 내용을 저장하지 못했습니다.")) }
        }
        job.invokeOnCompletion { contentSaveJobs.remove(id, job) }
        contentSaveJobs[id] = job
    }

    fun flushBundleContent(id: Long, content: String) {
        contentSaveJobs.remove(id)?.cancel()
        viewModelScope.launch {
            runCatching { repository.updateBundleContent(id, content) }
                .onFailure { _events.emit(UiEvent.Message("편집 내용을 저장하지 못했습니다.")) }
        }
    }

    fun togglePinned(id: Long) = action("고정 상태를 바꾸지 못했습니다.") { repository.togglePinned(id) }
    fun toggleArchived(id: Long) = action("보관 상태를 바꾸지 못했습니다.") { repository.toggleArchived(id) }
    fun deleteBundle(id: Long, onDone: () -> Unit = {}) = action("묶음을 삭제하지 못했습니다.") {
        repository.deleteBundle(id); onDone()
    }
    fun addMemos(bundleId: Long, ids: Set<Long>) = action("메모를 추가하지 못했습니다.") {
        repository.addMemos(bundleId, ids); _events.emit(UiEvent.Message("메모를 추가했습니다."))
    }
    fun removeMemo(bundleId: Long, memoId: Long) = action("메모를 빼지 못했습니다.") {
        repository.removeMemo(bundleId, memoId); _events.emit(UiEvent.Message("묶음에서만 제거했습니다."))
    }
    fun setCompactCards(value: Boolean) = action("설정을 저장하지 못했습니다.") { preferences.setCompactCards(value) }
    fun clearAll(onDone: () -> Unit = {}) = action("데이터를 삭제하지 못했습니다.") {
        repository.clearAll(); persistSelection(emptySet()); updateDraft(""); onDone()
    }

    fun exportBackup(resolver: android.content.ContentResolver, uri: android.net.Uri) = action("내보내기에 실패했습니다.") {
        val json = repository.encodeBackup(repository.exportBackup())
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("파일을 열 수 없습니다.")
        _events.emit(UiEvent.Message("데이터를 내보냈습니다."))
    }

    fun importBackupReplace(resolver: android.content.ContentResolver, uri: android.net.Uri) = action("가져오기에 실패했습니다.") {
        val json = resolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("파일을 열 수 없습니다.")
        repository.importReplace(json)
        persistSelection(emptySet())
        updateDraft("")
        _events.emit(UiEvent.Message("백업으로 데이터를 대체했습니다."))
    }

    fun notifyCopied() {
        viewModelScope.launch { _events.emit(UiEvent.Message("복사했습니다")) }
    }

    private fun readSelection(): Set<Long> =
        savedStateHandle.get<LongArray>(KEY_SELECTION)?.toSet() ?: emptySet()

    private fun persistSelection(ids: Set<Long>) {
        selection.value = ids
        savedStateHandle[KEY_SELECTION] = ids.toLongArray()
    }

    private fun action(error: String, block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { _events.emit(UiEvent.Message(it.message ?: error)) }
    }

    private companion object {
        const val KEY_DRAFT = "memo_draft"
        const val KEY_SELECTION = "memo_selection"
    }
}

fun buildTimeline(memos: List<MemoEntity>, bundles: List<BundleWithMemos>): List<TimelineItem> {
    data class Timed(val time: Long, val typeOrder: Int, val id: Long, val item: TimelineItem)
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN)
    val all = buildList {
        memos.forEach { add(Timed(it.createdAt, 0, it.id, TimelineItem.MemoItem(it))) }
        bundles.filterNot { it.bundle.isArchived }.forEach {
            add(Timed(it.bundle.createdAt, 1, it.bundle.id, TimelineItem.BundleItem(it)))
        }
    }.sortedWith(compareBy<Timed> { it.time }.thenBy { it.typeOrder }.thenBy { it.id })
    var lastDay: Long? = null
    return buildList {
        all.forEachIndexed { index, timed ->
            val date = Instant.ofEpochMilli(timed.time).atZone(zone).toLocalDate()
            if (date.toEpochDay() != lastDay) {
                add(TimelineItem.DateSeparatorItem(date.toEpochDay(), date.format(formatter)))
                lastDay = date.toEpochDay()
            }
            val item = timed.item
            if (item is TimelineItem.MemoItem) {
                val previous = all.getOrNull(index - 1)
                val next = all.getOrNull(index + 1)
                val startsGroup = previous?.item !is TimelineItem.MemoItem ||
                    !isSameLocalMinute(previous.time, timed.time, zone)
                val showTime = next?.item !is TimelineItem.MemoItem ||
                    !isSameLocalMinute(timed.time, next.time, zone)
                add(item.copy(startsGroup = startsGroup, showTime = showTime))
            } else {
                add(item)
            }
        }
    }
}

private fun isSameLocalMinute(first: Long, second: Long, zone: ZoneId): Boolean {
    val firstTime = Instant.ofEpochMilli(first).atZone(zone)
    val secondTime = Instant.ofEpochMilli(second).atZone(zone)
    return firstTime.toLocalDate() == secondTime.toLocalDate() &&
        firstTime.hour == secondTime.hour &&
        firstTime.minute == secondTime.minute
}
