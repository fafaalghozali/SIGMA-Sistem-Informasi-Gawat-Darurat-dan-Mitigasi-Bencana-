package com.mahasiswa.sigma.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mahasiswa.sigma.AuthData
import com.mahasiswa.sigma.DisasterReportsData
import com.mahasiswa.sigma.VolunteerData
import com.mahasiswa.sigma.data.datastore.AuthDataSerializer
import com.mahasiswa.sigma.data.datastore.DisasterReportsSerializer
import com.mahasiswa.sigma.data.datastore.VolunteerDataSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.authDataStore: DataStore<AuthData> by dataStore(
    fileName = "auth_data.pb",
    serializer = AuthDataSerializer
)

private val Context.disasterReportsDataStore: DataStore<DisasterReportsData> by dataStore(
    fileName = "disaster_reports.pb",
    serializer = DisasterReportsSerializer
)

private val Context.volunteerDataStore: DataStore<VolunteerData> by dataStore(
    fileName = "volunteer_data.pb",
    serializer = VolunteerDataSerializer
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @AuthDataStore
    fun provideAuthDataStore(@ApplicationContext context: Context): DataStore<AuthData> {
        return context.authDataStore
    }

    @Provides
    @Singleton
    @DisasterReportsDataStore
    fun provideDisasterReportsDataStore(
        @ApplicationContext context: Context
    ): DataStore<DisasterReportsData> {
        return context.disasterReportsDataStore
    }

    @Provides
    @Singleton
    @VolunteerDataStore
    fun provideVolunteerDataStore(
        @ApplicationContext context: Context
    ): DataStore<VolunteerData> {
        return context.volunteerDataStore
    }
}
