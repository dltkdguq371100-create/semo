package com.semo.memo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SemoDao {
    @Query("SELECT * FROM memos WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun observeActiveMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAllMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeletedMemos(): Flow<List<MemoEntity>>

    @Transaction
    @Query("SELECT * FROM bundles WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC, createdAt DESC")
    fun observeActiveBundles(): Flow<List<BundleWithMemos>>

    @Transaction
    @Query("SELECT * FROM bundles ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllBundles(): Flow<List<BundleWithMemos>>

    @Transaction
    @Query("SELECT * FROM bundles WHERE id = :id LIMIT 1")
    fun observeBundle(id: Long): Flow<BundleWithMemos?>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    suspend fun memo(id: Long): MemoEntity?

    @Query("SELECT * FROM memos WHERE id IN (:ids) AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun memos(ids: List<Long>): List<MemoEntity>

    @Query("SELECT * FROM bundles WHERE id = :id LIMIT 1")
    suspend fun bundle(id: Long): BundleEntity?

    @Insert suspend fun insertMemo(memo: MemoEntity): Long
    @Insert suspend fun insertBundle(bundle: BundleEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRefs(refs: List<BundleMemoCrossRef>)
    @Update suspend fun updateMemo(memo: MemoEntity)
    @Update suspend fun updateBundle(bundle: BundleEntity)

    @Query("DELETE FROM bundle_memo_refs WHERE bundleId = :bundleId AND memoId = :memoId")
    suspend fun removeMemoFromBundle(bundleId: Long, memoId: Long)

    @Query("DELETE FROM bundle_memo_refs WHERE bundleId = :bundleId")
    suspend fun deleteRefsForBundle(bundleId: Long)

    @Query("DELETE FROM bundles WHERE id = :bundleId")
    suspend fun deleteBundleRow(bundleId: Long)

    @Query("SELECT COUNT(*) FROM bundle_memo_refs WHERE memoId = :memoId")
    suspend fun bundleCountForMemo(memoId: Long): Int

    @Query("SELECT MAX(sortOrder) FROM bundle_memo_refs WHERE bundleId = :bundleId")
    suspend fun maxSortOrder(bundleId: Long): Long?

    @Query("DELETE FROM bundle_memo_refs WHERE memoId = :memoId")
    suspend fun deleteRefsForMemo(memoId: Long)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun permanentlyDeleteMemo(id: Long)

    @Query("DELETE FROM bundle_memo_refs") suspend fun clearRefs()
    @Query("DELETE FROM bundles") suspend fun clearBundles()
    @Query("DELETE FROM memos") suspend fun clearMemos()
}
