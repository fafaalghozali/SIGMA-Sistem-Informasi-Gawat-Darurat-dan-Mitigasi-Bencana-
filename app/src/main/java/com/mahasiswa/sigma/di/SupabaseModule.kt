package com.mahasiswa.sigma.di

import com.mahasiswa.sigma.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)

            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
            }
        }
    }
}
