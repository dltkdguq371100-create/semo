package com.semo.memo

import android.app.Application
import com.semo.memo.data.SemoDatabase
import com.semo.memo.data.SemoRepository
import com.semo.memo.data.UserPreferences

class SemoApplication : Application() {
    val database by lazy { SemoDatabase.get(this) }
    val repository by lazy { SemoRepository(database) }
    val preferences by lazy { UserPreferences(this) }
}
