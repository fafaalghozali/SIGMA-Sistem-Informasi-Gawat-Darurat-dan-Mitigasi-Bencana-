package com.mahasiswa.sigma.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mahasiswa.sigma.ui.viewmodel.VolunteerRegistrationData

class VolunteerRepository(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("volunteer_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveRegistration(username: String, data: VolunteerRegistrationData) {
        val json = gson.toJson(data)
        sharedPrefs.edit().putString("VOLUNTEER_REG_$username", json).apply()
    }

    fun getRegistration(username: String): VolunteerRegistrationData? {
        val json = sharedPrefs.getString("VOLUNTEER_REG_$username", null) ?: return null
        return try {
            gson.fromJson(json, VolunteerRegistrationData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearRegistration(username: String) {
        sharedPrefs.edit().remove("VOLUNTEER_REG_$username").apply()
    }
}
