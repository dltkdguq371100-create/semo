package com.semo.memo.ui

import com.semo.memo.data.BundleEntity
import com.semo.memo.data.BundleWithMemos
import com.semo.memo.data.MemoEntity
import com.semo.memo.data.TimelineItem
import com.semo.memo.data.displayTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    @Test
    fun timeline_isChronologicalAndAddsDateSeparator() {
        val first = MemoEntity(1, "첫 메모", 1_700_000_000_000, 1_700_000_000_000)
        val second = MemoEntity(2, "둘째 메모", 1_700_000_100_000, 1_700_000_100_000)

        val result = buildTimeline(listOf(second, first), emptyList())

        assertTrue(result.first() is TimelineItem.DateSeparatorItem)
        assertEquals(1L, (result[1] as TimelineItem.MemoItem).memo.id)
        assertEquals(2L, (result[2] as TimelineItem.MemoItem).memo.id)
    }

    @Test
    fun timeline_mergesMemosAndBundleByCreatedAt() {
        val base = (1_700_000_000_000L / 60_000L) * 60_000L
        val memos = listOf(
            MemoEntity(5, "오후 1:40", base + 140, base + 140),
            MemoEntity(1, "오전 11:20", base + 20, base + 20),
            MemoEntity(3, "오후 12:23", base + 83, base + 83),
            MemoEntity(2, "오전 11:21", base + 21, base + 21),
        )
        val bundle = BundleWithMemos(
            BundleEntity(4, "묶음", "정리", base + 130, base + 999),
            emptyList(),
        )

        val keys = buildTimeline(memos, listOf(bundle))
            .filterNot { it is TimelineItem.DateSeparatorItem }
            .map(TimelineItem::stableKey)

        assertEquals(listOf("memo-1", "memo-2", "memo-3", "bundle-4", "memo-5"), keys)
    }

    @Test
    fun equalTimestamps_useNumericIdsForStableMemoOrder() {
        val time = 1_700_000_000_000L
        val result = buildTimeline(
            listOf(
                MemoEntity(10, "열 번째", time, time),
                MemoEntity(2, "두 번째", time, time),
            ),
            emptyList(),
        ).filterIsInstance<TimelineItem.MemoItem>()

        assertEquals(listOf(2L, 10L), result.map { it.memo.id })
    }

    @Test
    fun sameMinuteMemoGroup_showsTimeOnlyOnLastAndBreaksAtBundleAndDate() {
        val base = (1_700_000_000_000L / 60_000L) * 60_000L
        val nextDay = base + 86_400_000L
        val memos = listOf(
            MemoEntity(1, "1", base, base),
            MemoEntity(2, "2", base + 10_000, base + 10_000),
            MemoEntity(3, "3", base + 30_000, base + 30_000),
            MemoEntity(4, "4", base + 40_000, base + 40_000),
            MemoEntity(5, "다음 날", nextDay, nextDay),
        )
        val bundle = BundleWithMemos(
            BundleEntity(9, "묶음", "정리", base + 20_000, base + 20_000),
            emptyList(),
        )

        val result = buildTimeline(memos, listOf(bundle))
        val memoItems = result.filterIsInstance<TimelineItem.MemoItem>()

        assertEquals(listOf(true, false, true, false, true), memoItems.map { it.startsGroup })
        assertEquals(listOf(false, true, false, true, true), memoItems.map { it.showTime })
        assertEquals(2, result.count { it is TimelineItem.DateSeparatorItem })
    }

    @Test
    fun bundleWithoutTitle_usesFirstMemoAndCount() {
        val memo1 = MemoEntity(1, "우유 주문", 100, 100)
        val memo2 = MemoEntity(2, "계란 주문", 200, 200)
        val bundle = BundleWithMemos(BundleEntity(3, null, "정리", 300, 300), listOf(memo2, memo1))

        assertEquals("우유 주문 외 1개", bundle.displayTitle())
    }

    @Test
    fun explicitBundleTitle_hasPriority() {
        val memo = MemoEntity(1, "원본", 100, 100)
        val bundle = BundleWithMemos(BundleEntity(3, " 장보기 ", "정리", 300, 300), listOf(memo))

        assertEquals("장보기", bundle.displayTitle())
    }

    @Test
    fun bundleCreatedEvent_carriesCreatedBundleId() {
        val event = UiEvent.BundleCreated(count = 3, id = 42L)

        assertEquals(3, event.count)
        assertEquals(42L, event.id)
    }

    @Test
    fun creationEventTargetsItsOwnTimelineItemInsteadOfAlwaysUsingLastItem() {
        val memo = MemoEntity(1, "메모", 100, 100)
        val bundle = BundleWithMemos(BundleEntity(2, "묶음", "정리", 200, 200), emptyList())
        val laterMemo = MemoEntity(3, "나중 메모", 300, 300)
        val timeline = listOf(
            TimelineItem.MemoItem(memo),
            TimelineItem.BundleItem(bundle),
            TimelineItem.MemoItem(laterMemo),
        )

        assertEquals(0, timelineIndexForEvent(timeline, UiEvent.MemoCreated(1)))
        assertEquals(1, timelineIndexForEvent(timeline, UiEvent.BundleCreated(1, 2)))
        assertEquals(-1, timelineIndexForEvent(timeline, UiEvent.Message("완료")))
    }
}
