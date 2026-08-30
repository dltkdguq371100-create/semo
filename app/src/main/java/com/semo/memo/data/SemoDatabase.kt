package com.semo.memo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

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
    version = 1,
    exportSchema = true,
)
abstract class SemoDatabase : RoomDatabase() {
    abstract fun dao(): SemoDao

    companion object {
        @Volatile private var instance: SemoDatabase? = null

        // Example for the next bump:
        // private val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE memos ADD COLUMN example TEXT NOT NULL DEFAULT ''")
        //     }
        // }
        private val ALL_MIGRATIONS: Array<Migration> = emptyArray()

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
