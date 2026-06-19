package com.mahasiswa.sigma.data.remote.api

import com.google.gson.annotations.SerializedName

data class OpenMeteoCurrentResponse(
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("timezone") val timezone: String = "",
    @SerializedName("current") val current: CurrentWeatherData? = null
)

data class CurrentWeatherData(
    @SerializedName("time") val time: String = "",
    @SerializedName("temperature_2m") val temperature2m: Double = 0.0,
    @SerializedName("weather_code") val weatherCode: Int = 0,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double = 0.0
)
