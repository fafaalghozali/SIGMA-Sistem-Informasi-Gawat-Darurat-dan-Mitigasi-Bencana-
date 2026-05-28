package com.mahasiswa.sigma.di

import android.content.Context
import androidx.room.Room
import com.mahasiswa.sigma.data.local.NewsDao
import com.mahasiswa.sigma.data.local.SigmaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSigmaDatabase(@ApplicationContext context: Context): SigmaDatabase {
        return Room.databaseBuilder(
            context,
            SigmaDatabase::class.java,
            "sigma_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNewsDao(database: SigmaDatabase): NewsDao {
        return database.newsDao()
    }
}
