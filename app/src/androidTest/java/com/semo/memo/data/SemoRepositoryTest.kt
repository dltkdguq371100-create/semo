package com.semo.memo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SemoRepositoryTest {
    private lateinit var db: SemoDatabase
    private lateinit var repository: SemoRepository

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SemoDatabase::class.java).allowMainThreadQueries().build()
        repository = SemoRepository(db)
    }

    @After @Throws(IOException::class) fun closeDatabase() = db.close()

    @Test fun createUpdateAndDeleteMemo() = runTest {
        val id = repository.createMemo("  테스트 메모  ")
        assertEquals("테스트 메모", repository.activeMemos.first().single().content)
        repository.updateMemo(id, "수정됨")
        assertEquals("수정됨", repository.activeMemos.first().single().content)
        repository.deleteMemo(id)
        assertTrue(repository.activeMemos.first().isEmpty())
    }

    @Test fun memoCanBelongToMultipleBundlesAndSurvivesBundleDeletion() = runTest {
        val first = repository.createMemo("첫 메모")
        val second = repository.createMemo("둘째 메모")
        val bundleA = repository.createBundle(setOf(first, second))
        repository.createBundle(setOf(first))
        assertEquals(2, repository.activeBundles.first().size)
        repository.deleteBundle(bundleA)
        assertEquals(2, repository.activeMemos.first().size)
        assertEquals(listOf(first), repository.activeBundles.first().single().memos.map { it.id })
    }

    @Test fun removingMemoFromBundleDoesNotDeleteOriginal() = runTest {
        val memo = repository.createMemo("원본")
        val bundle = repository.createBundle(setOf(memo))
        repository.removeMemo(bundle, memo)
        assertFalse(repository.activeMemos.first().isEmpty())
        assertTrue(repository.activeBundles.first().single().memos.isEmpty())
    }

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
