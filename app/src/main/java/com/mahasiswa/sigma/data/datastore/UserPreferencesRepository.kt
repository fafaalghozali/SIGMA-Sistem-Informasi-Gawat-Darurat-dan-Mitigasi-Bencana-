package com.mahasiswa.sigma.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_SAVED_EMAIL = stringPreferencesKey("saved_email")
        private val KEY_SAVED_PASSWORD = stringPreferencesKey("saved_password")
    }

    val rememberMe: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REMEMBER_ME] ?: false
    }

    val savedEmail: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_SAVED_EMAIL] ?: ""
    }

    val savedPassword: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_SAVED_PASSWORD] ?: ""
    }

    suspend fun saveCredentials(email: String, password: String) {
        dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = true
            prefs[KEY_SAVED_EMAIL] = email
            prefs[KEY_SAVED_PASSWORD] = password
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = false
            prefs[KEY_SAVED_EMAIL] = ""
            prefs[KEY_SAVED_PASSWORD] = ""
        }
    }
}
