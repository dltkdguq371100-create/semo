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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    val events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    var draft: String
        get() = savedStateHandle["memo_draft"] ?: ""
        set(value) { savedStateHandle["memo_draft"] = value }

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
    val allBundles = repository.allBundles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val compactCards = preferences.compactCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun bundle(id: Long) = repository.bundle(id)

    fun sendMemo(text: String, onSuccess: () -> Unit = {}) = action("메모를 저장하지 못했습니다.") {
        val id = repository.createMemo(text)
        draft = ""
        onSuccess()
        events.emit(UiEvent.MemoCreated(id))
    }

    fun updateMemo(id: Long, text: String) = action("메모를 수정하지 못했습니다.") {
        repository.updateMemo(id, text)
        events.emit(UiEvent.Message("메모를 수정했습니다."))
    }

    fun deleteMemo(id: Long) = action("메모를 삭제하지 못했습니다.") {
        val wasLinked = repository.deleteMemo(id)
        events.emit(UiEvent.Message(if (wasLinked) "묶음 연결과 원본 메모를 삭제했습니다." else "메모를 삭제했습니다."))
    }

    fun toggleSelection(id: Long) {
        selection.value = selection.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun clearSelection() { selection.value = emptySet() }

    fun bundleSelected() = action("묶음을 만들지 못했습니다.") {
        val ids = selection.value
        if (ids.isEmpty()) return@action
        val id = repository.createBundle(ids)
        selection.value = emptySet()
        events.emit(UiEvent.BundleCreated(ids.size, id))
    }

    fun updateBundleTitle(id: Long, title: String) = action("제목을 저장하지 못했습니다.") {
        repository.updateBundleTitle(id, title)
    }

    private val contentSaveJobs = mutableMapOf<Long, Job>()

    fun updateBundleContentDebounced(id: Long, content: String) {
        contentSaveJobs[id]?.cancel()
        val job = viewModelScope.launch {
            delay(450)
            runCatching { repository.updateBundleContent(id, content) }
                .onFailure { events.emit(UiEvent.Message("편집 내용을 저장하지 못했습니다.")) }
        }
        job.invokeOnCompletion { contentSaveJobs.remove(id, job) }
        contentSaveJobs[id] = job
    }

    fun flushBundleContent(id: Long, content: String) {
        contentSaveJobs.remove(id)?.cancel()
        viewModelScope.launch {
            runCatching { repository.updateBundleContent(id, content) }
                .onFailure { events.emit(UiEvent.Message("편집 내용을 저장하지 못했습니다.")) }
        }
    }

    fun togglePinned(id: Long) = action("고정 상태를 바꾸지 못했습니다.") { repository.togglePinned(id) }
    fun toggleArchived(id: Long) = action("보관 상태를 바꾸지 못했습니다.") { repository.toggleArchived(id) }
    fun deleteBundle(id: Long, onDone: () -> Unit = {}) = action("묶음을 삭제하지 못했습니다.") {
        repository.deleteBundle(id); onDone()
    }
    fun addMemos(bundleId: Long, ids: Set<Long>) = action("메모를 추가하지 못했습니다.") {
        repository.addMemos(bundleId, ids); events.emit(UiEvent.Message("메모를 추가했습니다."))
    }
    fun removeMemo(bundleId: Long, memoId: Long) = action("메모를 빼지 못했습니다.") {
        repository.removeMemo(bundleId, memoId); events.emit(UiEvent.Message("묶음에서만 제거했습니다."))
    }
    fun setCompactCards(value: Boolean) = action("설정을 저장하지 못했습니다.") { preferences.setCompactCards(value) }
    fun clearAll(onDone: () -> Unit = {}) = action("데이터를 삭제하지 못했습니다.") {
        repository.clearAll(); selection.value = emptySet(); draft = ""; onDone()
    }

    private fun action(error: String, block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { events.emit(UiEvent.Message(it.message ?: error)) }
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
