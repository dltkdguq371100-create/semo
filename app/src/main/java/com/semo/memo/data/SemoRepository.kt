package com.semo.memo.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class SemoRepository(private val database: SemoDatabase) {
    private val dao = database.dao()

    val activeMemos: Flow<List<MemoEntity>> = dao.observeActiveMemos()
    val allMemos: Flow<List<MemoEntity>> = dao.observeAllMemos()
    val activeBundles: Flow<List<BundleWithMemos>> = dao.observeActiveBundles()
    val allBundles: Flow<List<BundleWithMemos>> = dao.observeAllBundles()

    fun bundle(id: Long) = dao.observeBundle(id)

    suspend fun createMemo(raw: String): Long {
        val content = raw.trim()
        require(content.isNotEmpty()) { "빈 메모는 저장할 수 없습니다." }
        val now = System.currentTimeMillis()
        return dao.insertMemo(MemoEntity(content = content, createdAt = now, updatedAt = now))
    }

    suspend fun updateMemo(id: Long, raw: String) {
        val content = raw.trim()
        require(content.isNotEmpty()) { "빈 메모는 저장할 수 없습니다." }
        dao.memo(id)?.let { dao.updateMemo(it.copy(content = content, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun deleteMemo(id: Long): Boolean = database.withTransaction {
        val linked = dao.bundleCountForMemo(id) > 0
        dao.memo(id)?.let {
            dao.deleteRefsForMemo(id)
            dao.updateMemo(it.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        }
        linked
    }

    suspend fun createBundle(memoIds: Set<Long>): Long = database.withTransaction {
        require(memoIds.isNotEmpty()) { "하나 이상의 메모를 선택하세요." }
        val source = dao.memos(memoIds.toList()).sortedBy { it.createdAt }
        require(source.isNotEmpty()) { "선택한 메모를 찾을 수 없습니다." }
        val now = System.currentTimeMillis()
        val id = dao.insertBundle(
            BundleEntity(
                editableContent = source.joinToString("\n") { it.content },
                createdAt = now,
                updatedAt = now,
            ),
        )
        dao.insertRefs(source.mapIndexed { index, memo ->
            BundleMemoCrossRef(id, memo.id, now, index.toLong())
        })
        id
    }

    suspend fun updateBundleTitle(id: Long, title: String) {
        dao.bundle(id)?.let {
            dao.updateBundle(it.copy(title = title.trim().ifEmpty { null }, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateBundleContent(id: Long, content: String) {
        dao.bundle(id)?.let { dao.updateBundle(it.copy(editableContent = content, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun togglePinned(id: Long) {
        dao.bundle(id)?.let { dao.updateBundle(it.copy(isPinned = !it.isPinned, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun toggleArchived(id: Long) {
        dao.bundle(id)?.let { dao.updateBundle(it.copy(isArchived = !it.isArchived, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun addMemos(bundleId: Long, memoIds: Set<Long>) = database.withTransaction {
        val now = System.currentTimeMillis()
        val source = dao.memos(memoIds.toList())
        dao.insertRefs(source.mapIndexed { index, memo ->
            BundleMemoCrossRef(bundleId, memo.id, now, now + index)
        })
        dao.bundle(bundleId)?.let { dao.updateBundle(it.copy(updatedAt = now)) }
    }

    suspend fun removeMemo(bundleId: Long, memoId: Long) = database.withTransaction {
        dao.removeMemoFromBundle(bundleId, memoId)
        dao.bundle(bundleId)?.let { dao.updateBundle(it.copy(updatedAt = System.currentTimeMillis())) }
    }

    suspend fun deleteBundle(id: Long) = database.withTransaction {
        dao.deleteRefsForBundle(id)
        dao.deleteBundleRow(id)
    }

    suspend fun clearAll() = database.withTransaction {
        dao.clearRefs(); dao.clearBundles(); dao.clearMemos()
    }
}
