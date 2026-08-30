package com.semo.memo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema policy: prefer explicit [Migration] objects for every version bump.
 *
 * This is a personal offline app without a first-class export path yet, so
 * [RoomDatabase.Builder.fallbackToDestructiveMigration] is intentionally
 * omitted — a missing migration should fail loudly rather than wipe memos.
 * Wire new migrations into [ALL_MIGRATIONS] and bump [version] together.
 */
@Database(
    entities = [MemoEntity::class, BundleEntity::class, BundleMemoCrossRef::class],
    version = 2,
    exportSchema = true,
)
abstract class SemoDatabase : RoomDatabase() {
    abstract fun dao(): SemoDao

    companion object {
        @Volatile private var instance: SemoDatabase? = null

        // Drop unused MemoEntity.isArchived (never set true; archive is bundle-only).
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memos_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `memos_new` (`id`, `content`, `createdAt`, `updatedAt`, `isDeleted`)
                    SELECT `id`, `content`, `createdAt`, `updatedAt`, `isDeleted` FROM `memos`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `memos`")
                db.execSQL("ALTER TABLE `memos_new` RENAME TO `memos`")
            }
        }

        private val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun get(context: Context): SemoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SemoDatabase::class.java,
                "semo.db",
            )
                .addMigrations(*ALL_MIGRATIONS)
                .build().also { instance = it }
        }
    }
}
