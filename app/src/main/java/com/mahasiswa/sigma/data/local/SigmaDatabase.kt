package com.mahasiswa.sigma.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * SIGMA Room database.
 *
 * Version 1 — disaster_news table only.
 * Future versions can add user reports, evacuation routes, etc.
 */
@Database(
    entities = [NewsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SigmaDatabase : RoomDatabase() {

    abstract fun newsDao(): NewsDao

    companion object {
        private const val DB_NAME = "sigma_db"

        @Volatile
        private var INSTANCE: SigmaDatabase? = null

        /**
         * Returns the singleton database instance, creating it if necessary.
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): SigmaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context): SigmaDatabase =
            Room.databaseBuilder(context, SigmaDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()   // safe for cache-only data
                .build()
    }
}
