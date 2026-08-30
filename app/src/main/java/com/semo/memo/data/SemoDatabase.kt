package com.semo.memo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MemoEntity::class, BundleEntity::class, BundleMemoCrossRef::class],
    version = 1,
    exportSchema = true,
)
abstract class SemoDatabase : RoomDatabase() {
    abstract fun dao(): SemoDao

    companion object {
        @Volatile private var instance: SemoDatabase? = null

        fun get(context: Context): SemoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SemoDatabase::class.java,
                "semo.db",
            ).build().also { instance = it }
        }
    }
}
