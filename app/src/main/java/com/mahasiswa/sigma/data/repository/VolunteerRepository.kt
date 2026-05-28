package com.mahasiswa.sigma.data.repository

import androidx.datastore.core.DataStore
import com.mahasiswa.sigma.VolunteerData
import com.mahasiswa.sigma.VolunteerEntry
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.di.VolunteerDataStore
import com.mahasiswa.sigma.ui.viewmodel.VolunteerRegistrationData
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolunteerRepository @Inject constructor(
    @VolunteerDataStore private val dataStore: DataStore<VolunteerData>
) {

    suspend fun saveRegistration(username: String, data: VolunteerRegistrationData) {
        dataStore.updateData { currentData ->
            val newEntry = VolunteerEntry.newBuilder()
                .setUsername(username)
                .setName(data.name)
                .setSkill(data.skill.name)
                .setAddress(data.address)
                .setPhoneNumber(data.phoneNumber)
                .setStatus(data.status)
                .build()

            val existingIndex = currentData.registrationsList.indexOfFirst { it.username == username }
            if (existingIndex != -1) {
                currentData.toBuilder()
                    .setRegistrations(existingIndex, newEntry)
                    .build()
            } else {
                currentData.toBuilder()
                    .addRegistrations(newEntry)
                    .build()
            }
        }
    }

    suspend fun getRegistration(username: String): VolunteerRegistrationData? {
        val data = dataStore.data.first()
        val entry = data.registrationsList.find { it.username == username } ?: return null
        return try {
            VolunteerRegistrationData(
                name = entry.name,
                skill = SkillsVolunteer.valueOf(entry.skill),
                address = entry.address,
                phoneNumber = entry.phoneNumber,
                status = entry.status
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearRegistration(username: String) {
        dataStore.updateData { currentData ->
            val index = currentData.registrationsList.indexOfFirst { it.username == username }
            if (index == -1) return@updateData currentData
            currentData.toBuilder()
                .removeRegistrations(index)
                .build()
        }
    }

    suspend fun updateVolunteerStatus(username: String, newStatus: String) {
        dataStore.updateData { currentData ->
            val index = currentData.registrationsList.indexOfFirst { it.username == username }
            if (index == -1) return@updateData currentData

            val updatedEntry = currentData.registrationsList[index].toBuilder()
                .setStatus(newStatus)
                .build()

            currentData.toBuilder()
                .setRegistrations(index, updatedEntry)
                .build()
        }
    }

    suspend fun getAllRegistrations(): List<VolunteerEntry> {
        return dataStore.data.first().registrationsList
    }
}

