package com.semo.memo.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)

@Entity(tableName = "bundles")
data class BundleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String? = null,
    val editableContent: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "bundle_memo_refs",
    primaryKeys = ["bundleId", "memoId"],
    indices = [Index("memoId"), Index("bundleId")],
)
data class BundleMemoCrossRef(
    val bundleId: Long,
    val memoId: Long,
    val addedAt: Long,
    val sortOrder: Long,
)

data class BundleWithMemos(
    @Embedded val bundle: BundleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BundleMemoCrossRef::class,
            parentColumn = "bundleId",
            entityColumn = "memoId",
        ),
    )
    val memos: List<MemoEntity>,
)

sealed interface TimelineItem {
    val stableKey: String

    data class MemoItem(
        val memo: MemoEntity,
        val startsGroup: Boolean = true,
        val showTime: Boolean = true,
    ) : TimelineItem {
        override val stableKey = "memo-${memo.id}"
    }

    data class BundleItem(val value: BundleWithMemos) : TimelineItem {
        override val stableKey = "bundle-${value.bundle.id}"
    }

    data class DateSeparatorItem(val epochDay: Long, val label: String) : TimelineItem {
        override val stableKey = "date-$epochDay"
    }
}

fun BundleWithMemos.displayTitle(): String {
    val explicit = bundle.title?.trim().orEmpty()
    if (explicit.isNotEmpty()) return explicit
    val first = memos.minByOrNull { it.createdAt }?.content?.lineSequence()?.firstOrNull()?.trim().orEmpty()
    return when {
        first.isEmpty() -> "제목 없는 묶음"
        memos.size > 1 -> "$first 외 ${memos.size - 1}개"
        else -> first
    }
}

fun BundleWithMemos.previewText(): String = memos
    .sortedBy { it.createdAt }
    .take(3)
    .joinToString(" · ") { it.content.lineSequence().joinToString(" ").trim() }
    .ifBlank { bundle.editableContent.ifBlank { "원본 메모 없음" } }
