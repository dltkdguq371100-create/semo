package com.semo.memo.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class SemoRepository(private val database: SemoDatabase) {
    private val dao = database.dao()

    val activeMemos: Flow<List<MemoEntity>> = dao.observeActiveMemos()
    val allMemos: Flow<List<MemoEntity>> = dao.observeAllMemos()
    val deletedMemos: Flow<List<MemoEntity>> = dao.observeDeletedMemos()
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

    /** Restores a soft-deleted memo. Bundle links removed at delete time are not recreated. */
    suspend fun restoreMemo(id: Long) {
        dao.memo(id)?.let {
            require(it.isDeleted) { "휴지통에 없는 메모입니다." }
            dao.updateMemo(it.copy(isDeleted = false, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun permanentlyDeleteMemo(id: Long) = database.withTransaction {
        dao.deleteRefsForMemo(id)
        dao.permanentlyDeleteMemo(id)
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
        // Keep sortOrder on the same 0,1,2… scale as createBundle (not epoch millis).
        var nextOrder = (dao.maxSortOrder(bundleId) ?: -1L) + 1L
        dao.insertRefs(source.map { memo ->
            BundleMemoCrossRef(bundleId, memo.id, now, nextOrder++)
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

    suspend fun exportBackup(): SemoBackup = SemoBackup(
        schemaVersion = BACKUP_SCHEMA_VERSION,
        exportedAt = System.currentTimeMillis(),
        memos = dao.allMemosSnapshot(),
        bundles = dao.allBundlesSnapshot(),
        refs = dao.allRefsSnapshot(),
    )

    fun encodeBackup(backup: SemoBackup): String = SemoBackupCodec.encode(backup)

    /** Full replace: wipe local rows then insert the backup inside one transaction. */
    suspend fun importReplace(json: String) {
        val backup = SemoBackupCodec.decode(json)
        database.withTransaction {
            dao.clearRefs()
            dao.clearBundles()
            dao.clearMemos()
            if (backup.memos.isNotEmpty()) dao.insertMemos(backup.memos)
            if (backup.bundles.isNotEmpty()) dao.insertBundles(backup.bundles)
            if (backup.refs.isNotEmpty()) dao.replaceRefs(backup.refs)
            syncSqliteSequence("memos", backup.memos.maxOfOrNull { it.id } ?: 0L)
            syncSqliteSequence("bundles", backup.bundles.maxOfOrNull { it.id } ?: 0L)
        }
    }

    private fun syncSqliteSequence(table: String, maxId: Long) {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf<Any>(table))
        if (maxId > 0L) {
            db.execSQL(
                "INSERT INTO sqlite_sequence(name, seq) VALUES (?, ?)",
                arrayOf<Any>(table, maxId),
            )
        }
    }
}
