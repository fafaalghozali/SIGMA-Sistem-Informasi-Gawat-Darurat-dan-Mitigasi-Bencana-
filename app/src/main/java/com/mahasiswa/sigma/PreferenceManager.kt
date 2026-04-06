package com.mahasiswa.sigma

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SIGMA_PREFS", Context.MODE_PRIVATE)

    fun saveUser(name: String, email: String, role: String) {
        sharedPreferences.edit().apply {
            putString("USER_NAME", name)
            putString("USER_EMAIL", email)
            putString("USER_ROLE", role)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }
    }

    fun getUserName(): String? = sharedPreferences.getString("USER_NAME", null)
    fun getUserRole(): String? = sharedPreferences.getString("USER_ROLE", null)
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean("IS_LOGGED_IN", false)

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
