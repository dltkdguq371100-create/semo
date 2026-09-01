package com.semo.memo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("semo_preferences")

private const val MAX_RECENT_SEARCHES = 8

class UserPreferences(private val context: Context) {
    private val compactKey = booleanPreferencesKey("compact_bundle_cards")
    // Search input is single-line, so a newline-joined string is a safe encoding.
    private val recentSearchesKey = stringPreferencesKey("recent_searches")

    val compactCards: Flow<Boolean> = context.dataStore.data.map { it[compactKey] ?: false }
    suspend fun setCompactCards(value: Boolean) = context.dataStore.edit { it[compactKey] = value }

    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[recentSearchesKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun addRecentSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[recentSearchesKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(query) + current.filterNot { it.equals(query, ignoreCase = true) })
                .take(MAX_RECENT_SEARCHES)
            prefs[recentSearchesKey] = updated.joinToString("\n")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[recentSearchesKey]?.split('\n')?.filter { it.isNotBlank() } ?: return@edit
            prefs[recentSearchesKey] = current.filterNot { it == query }.joinToString("\n")
        }
    }
}
