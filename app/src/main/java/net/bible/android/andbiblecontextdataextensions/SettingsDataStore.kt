package net.bible.android.andbiblecontextdataextensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {
    private val dataStore = context.dataStore

    val linebreaks: Flow<Boolean> = dataStore.data.map {
        it[LINEBREAKS] ?: true
    }

    val pilcrows: Flow<Boolean> = dataStore.data.map {
        it[PILCROWS] ?: true
    }

    val verseNumbers: Flow<Boolean> = dataStore.data.map {
        it[VERSE_NUMBERS] ?: true
    }

    val chevrons: Flow<Boolean> = dataStore.data.map {
        it[CHEVRONS] ?: true
    }

    val brackets: Flow<Boolean> = dataStore.data.map {
        it[BRACKETS] ?: true
    }

    suspend fun saveLinebreaks(value: Boolean) {
        dataStore.edit { it[LINEBREAKS] = value }
    }

    suspend fun savePilcrows(value: Boolean) {
        dataStore.edit { it[PILCROWS] = value }
    }

    suspend fun saveVerseNumbers(value: Boolean) {
        dataStore.edit { it[VERSE_NUMBERS] = value }
    }

    suspend fun saveChevrons(value: Boolean) {
        dataStore.edit { it[CHEVRONS] = value }
    }

    suspend fun saveBrackets(value: Boolean) {
        dataStore.edit { it[BRACKETS] = value }
    }

    companion object {
        val LINEBREAKS = booleanPreferencesKey("linebreaks")
        val PILCROWS = booleanPreferencesKey("pilcrows")
        val VERSE_NUMBERS = booleanPreferencesKey("verse_numbers")
        val CHEVRONS = booleanPreferencesKey("chevrons")
        val BRACKETS = booleanPreferencesKey("brackets")
    }
}
