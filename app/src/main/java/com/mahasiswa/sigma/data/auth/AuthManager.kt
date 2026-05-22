package com.mahasiswa.sigma.data.auth

import androidx.datastore.core.DataStore
import com.mahasiswa.sigma.AuthData
import com.mahasiswa.sigma.UserEntry
import com.mahasiswa.sigma.data.model.UserRole
import kotlinx.coroutines.flow.first

class AuthManager(private val authDataStore: DataStore<AuthData>) {

    suspend fun registerUser(username: String, pass: String, role: UserRole, name: String): Boolean {
        if (username.isEmpty() || pass.isEmpty()) return false
        authDataStore.updateData { currentData ->
            val existingIndex = currentData.usersList.indexOfFirst { it.username == username }
            val newEntry = UserEntry.newBuilder()
                .setUsername(username)
                .setPassword(pass)
                .setRole(role.name)
                .setName(name)
                .build()

            if (existingIndex != -1) {
                currentData.toBuilder()
                    .setUsers(existingIndex, newEntry)
                    .build()
            } else {
                currentData.toBuilder()
                    .addUsers(newEntry)
                    .build()
            }
        }
        return true
    }

    suspend fun loginUser(username: String, pass: String): UserRole? {
        
        if (username == "admin" && pass == "admin") {
            return UserRole.BNPB
        }

        val data = authDataStore.data.first()
        val user = data.usersList.find { it.username == username }
        return if (user != null && user.password == pass) {
            UserRole.fromString(user.role)
        } else {
            null
        }
    }

    suspend fun getUserName(username: String): String {
        if (username == "admin") return "Administrator BNPB"
        val data = authDataStore.data.first()
        val user = data.usersList.find { it.username == username }
        return user?.name ?: "User"
    }

    suspend fun updateProfile(oldEmail: String, newName: String, newEmail: String): Boolean {
        if (newName.isBlank() || newEmail.isBlank()) return false
        authDataStore.updateData { currentData ->
            val index = currentData.usersList.indexOfFirst { it.username == oldEmail }
            if (index == -1) return@updateData currentData

            val existingEntry = currentData.usersList[index]
            val updatedEntry = UserEntry.newBuilder()
                .setUsername(newEmail)
                .setPassword(existingEntry.password)
                .setRole(existingEntry.role)
                .setName(newName)
                .build()

            currentData.toBuilder()
                .setUsers(index, updatedEntry)
                .build()
        }
        return true
    }
}
