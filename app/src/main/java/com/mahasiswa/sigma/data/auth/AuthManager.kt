package com.mahasiswa.sigma.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.mahasiswa.sigma.data.model.UserRole

class AuthManager(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("sigma_prefs", Context.MODE_PRIVATE)

    fun registerUser(username: String, pass: String, role: UserRole, name: String): Boolean {
        if (username.isEmpty() || pass.isEmpty()) return false
        sharedPrefs.edit().apply {
            putString("USER_$username", pass)
            putString("ROLE_$username", role.name)
            putString("NAME_$username", name)
            apply()
        }
        return true
    }

    fun loginUser(username: String, pass: String): UserRole? {
        val savedPass = sharedPrefs.getString("USER_$username", null)
        return if (savedPass != null && savedPass == pass) {
            val roleName = sharedPrefs.getString("ROLE_$username", UserRole.MASYARAKAT.name)
            UserRole.fromString(roleName)
        } else {
            null 
        }
    }

    fun getUserName(username: String): String {
        return sharedPrefs.getString("NAME_$username", "User") ?: "User"
    }

    fun updateProfile(oldEmail: String, newName: String, newEmail: String): Boolean {
        if (newName.isBlank() || newEmail.isBlank()) return false
        
        val password = sharedPrefs.getString("USER_$oldEmail", "") ?: ""
        val role = sharedPrefs.getString("ROLE_$oldEmail", UserRole.MASYARAKAT.name) ?: UserRole.MASYARAKAT.name
        
        sharedPrefs.edit().apply {
            if (oldEmail != newEmail) {
                remove("USER_$oldEmail")
                remove("ROLE_$oldEmail")
                remove("NAME_$oldEmail")
                
                putString("USER_$newEmail", password)
                putString("ROLE_$newEmail", role)
                putString("NAME_$newEmail", newName)
            } else {
                putString("NAME_$newEmail", newName)
            }
            apply()
        }
        return true
    }
}
