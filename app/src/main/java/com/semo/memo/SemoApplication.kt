package com.semo.memo

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.semo.memo.data.SemoDatabase
import com.semo.memo.data.SemoRepository
import com.semo.memo.data.UserPreferences
import com.semo.memo.widget.SemoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SemoApplication : Application() {
    val database by lazy { SemoDatabase.get(this) }
    val repository by lazy { SemoRepository(database) }
    val preferences by lazy { UserPreferences(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // All memo writes happen in this process, so refreshing the widget on
        // each emission keeps it current without periodic updates.
        applicationScope.launch {
            repository.activeMemos.collect {
                runCatching { SemoWidget().updateAll(this@SemoApplication) }
            }
        }
    }
}
