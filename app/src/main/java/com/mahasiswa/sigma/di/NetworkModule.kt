package com.mahasiswa.sigma.di

import com.google.gson.GsonBuilder
import com.mahasiswa.sigma.BuildConfig
import com.mahasiswa.sigma.data.remote.api.BmkgApiService
import com.mahasiswa.sigma.data.remote.api.OpenMeteoApiService
import com.mahasiswa.sigma.data.remote.api.RssApiService
import com.mahasiswa.sigma.data.remote.api.SupabaseApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BMKG_BASE_URL = "https://data.bmkg.go.id/"
    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"
    private const val RSS_BASE_URL = "https://www.antaranews.com/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("bmkg")
    fun provideBmkgRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BMKG_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("openmeteo")
    fun provideOpenMeteoRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OPEN_METEO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("rss")
    fun provideRssRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RSS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBmkgApiService(@Named("bmkg") retrofit: Retrofit): BmkgApiService {
        return retrofit.create(BmkgApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(@Named("openmeteo") retrofit: Retrofit): OpenMeteoApiService {
        return retrofit.create(OpenMeteoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRssApiService(@Named("rss") retrofit: Retrofit): RssApiService {
        return retrofit.create(RssApiService::class.java)
    }

    // ==================== SUPABASE REST API ====================

    /**
     * Supabase-specific OkHttpClient with authentication headers
     */
    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()
            chain.proceed(newRequest)
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Supabase PostgREST Retrofit instance
     */
    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseRetrofit(@Named("supabase") okHttpClient: OkHttpClient): Retrofit {
        // Supabase PostgREST endpoint is at /rest/v1/
        val supabaseRestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/"

        // Gson with custom deserializer for VolunteerReportDto's report_data field
        // which can be either a JSON string or a JSON object (jsonb column in Supabase)
        val gson = GsonBuilder()
            .registerTypeAdapter(
                com.mahasiswa.sigma.data.model.VolunteerReportDto::class.java,
                com.mahasiswa.sigma.data.model.VolunteerReportDtoDeserializer()
            )
            .create()

        return Retrofit.Builder()
            .baseUrl(supabaseRestUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Supabase API Service for database operations via REST
     */
    @Provides
    @Singleton
    fun provideSupabaseApiService(@Named("supabase") retrofit: Retrofit): SupabaseApiService {
        return retrofit.create(SupabaseApiService::class.java)
    }
}
