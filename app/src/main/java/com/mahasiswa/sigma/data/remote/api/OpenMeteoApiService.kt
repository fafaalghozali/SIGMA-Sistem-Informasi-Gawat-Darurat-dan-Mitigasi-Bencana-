package com.mahasiswa.sigma.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m",
        @Query("timezone") timezone: String = "Asia/Jakarta"
    ): OpenMeteoCurrentResponse
}
