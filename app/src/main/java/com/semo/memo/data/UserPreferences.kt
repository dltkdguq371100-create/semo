package com.semo.memo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("semo_preferences")

class UserPreferences(private val context: Context) {
    private val compactKey = booleanPreferencesKey("compact_bundle_cards")
    val compactCards: Flow<Boolean> = context.dataStore.data.map { it[compactKey] ?: false }
    suspend fun setCompactCards(value: Boolean) = context.dataStore.edit { it[compactKey] = value }
}
