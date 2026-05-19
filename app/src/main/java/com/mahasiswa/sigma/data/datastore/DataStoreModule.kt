package com.mahasiswa.sigma.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mahasiswa.sigma.AuthData
import com.mahasiswa.sigma.DisasterReportsData
import com.mahasiswa.sigma.VolunteerData

val Context.authDataStore: DataStore<AuthData> by dataStore(
    fileName = "auth_data.pb",
    serializer = AuthDataSerializer
)

val Context.disasterReportsDataStore: DataStore<DisasterReportsData> by dataStore(
    fileName = "disaster_reports.pb",
    serializer = DisasterReportsSerializer
)

val Context.volunteerDataStore: DataStore<VolunteerData> by dataStore(
    fileName = "volunteer_data.pb",
    serializer = VolunteerDataSerializer
)
