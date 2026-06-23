package com.mahasiswa.sigma.data.remote.api

import com.mahasiswa.sigma.data.model.*
import retrofit2.http.*

/**
 * Supabase REST API Service using PostgREST
 * 
 * All endpoints follow Supabase PostgREST convention:
 * - GET with filters: Use query parameters with column names (e.g., ?id=eq.123)
 * - POST: Insert new record
 * - PATCH: Update existing record (with filters)
 * - DELETE: Delete record (with filters)
 * 
 * Headers required:
 * - apikey: Supabase anon key
 * - Authorization: Bearer token
 * - Prefer: return=representation (for POST/PATCH to get response body)
 */
interface SupabaseApiService {

    // ==================== PROFILES ====================
    
    @GET("profiles")
    suspend fun getProfiles(
        @Query("select") select: String = "*"
    ): List<ProfileDto>

    @GET("profiles")
    suspend fun getProfileById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<ProfileDto>
    
    @GET("profiles")
    suspend fun getProfileByEmail(
        @Query("email") email: String,
        @Query("select") select: String = "*"
    ): List<ProfileDto>

    @POST("profiles")
    suspend fun createProfile(@Body profile: CreateProfileRequest): List<ProfileDto>

    @PATCH("profiles")
    suspend fun updateProfile(
        @Query("id") id: String,
        @Body updates: UpdateProfileRequest
    ): List<ProfileDto>

    @DELETE("profiles")
    suspend fun deleteProfile(
        @Query("id") id: String
    )

    // ==================== VOLUNTEERS ====================
    
    @GET("volunteers")
    suspend fun getVolunteers(
        @Query("select") select: String = "*"
    ): List<VolunteerDto>

    @GET("volunteers")
    suspend fun getVolunteerById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<VolunteerDto>
    
    @GET("volunteers")
    suspend fun getVolunteerByUserId(
        @Query("user_id") userId: String,
        @Query("select") select: String = "*"
    ): List<VolunteerDto>

    @POST("volunteers")
    suspend fun createVolunteer(@Body volunteer: CreateVolunteerRequest): List<VolunteerDto>

    @PATCH("volunteers")
    suspend fun updateVolunteer(
        @Query("id") id: String,
        @Body updates: UpdateVolunteerRequest
    ): List<VolunteerDto>

    @PATCH("volunteers")
    suspend fun updateVolunteerMap(
        @Query("id") id: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): List<VolunteerDto>

    @DELETE("volunteers")
    suspend fun deleteVolunteer(
        @Query("id") id: String
    )

    // ==================== DISASTERS (Disaster Reports) ====================
    
    @GET("disasters")
    suspend fun getDisasterReports(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<DisasterReportDto>

    @GET("disasters")
    suspend fun getDisasterReportById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<DisasterReportDto>
    
    @GET("disasters")
    suspend fun getDisasterReportsByStatus(
        @Query("status") status: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<DisasterReportDto>

    @POST("disasters")
    suspend fun createDisasterReport(@Body report: CreateDisasterReportRequest): List<DisasterReportDto>

    @PATCH("disasters")
    suspend fun updateDisasterReport(
        @Query("id") id: String,
        @Body updates: UpdateDisasterReportRequest
    ): List<DisasterReportDto>

    @DELETE("disasters")
    suspend fun deleteDisasterReport(
        @Query("id") id: String
    )

    // ==================== SHELTERS ====================
    
    @GET("shelters")
    suspend fun getShelters(
        @Query("select") select: String = "*"
    ): List<ShelterDto>

    @GET("shelters")
    suspend fun getShelterById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<ShelterDto>

    @POST("shelters")
    suspend fun createShelter(@Body shelter: CreateShelterRequest): List<ShelterDto>

    @PATCH("shelters")
    suspend fun updateShelter(
        @Query("id") id: String,
        @Body updates: UpdateShelterRequest
    ): List<ShelterDto>

    @DELETE("shelters")
    suspend fun deleteShelter(
        @Query("id") id: String
    )

    // ==================== VOLUNTEERS (filtered) ====================

    @GET("volunteers")
    suspend fun getVolunteersByDisasterId(
        @Query("disaster_id") disasterId: String,
        @Query("select") select: String = "*"
    ): List<VolunteerDto>

    // ==================== VOLUNTEER REPORTS ====================
    
    @GET("volunteer_reports")
    suspend fun getVolunteerReports(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<VolunteerReportDto>

    @GET("volunteer_reports")
    suspend fun getVolunteerReportsByDisasterId(
        @Query("disaster_id") disasterId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<VolunteerReportDto>

    @GET("volunteer_reports")
    suspend fun getVolunteerReportById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<VolunteerReportDto>
    
    @GET("volunteer_reports")
    suspend fun getVolunteerReportsByVolunteerId(
        @Query("volunteer_id") volunteerId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<VolunteerReportDto>

    @POST("volunteer_reports")
    suspend fun createVolunteerReport(@Body report: CreateVolunteerReportRequest): List<VolunteerReportDto>

    @PATCH("volunteer_reports")
    suspend fun updateVolunteerReport(
        @Query("id") id: String,
        @Body updates: Map<String, Any?>
    ): List<VolunteerReportDto>

    @DELETE("volunteer_reports")
    suspend fun deleteVolunteerReport(
        @Query("id") id: String
    )
    
    // ==================== NEWS ====================
    
    @GET("news")
    suspend fun getNews(
        @Query("select") select: String = "*",
        @Query("order") order: String = "published_at.desc"
    ): List<NewsDto>

    @GET("news")
    suspend fun getNewsById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<NewsDto>

    @POST("news")
    suspend fun createNews(@Body news: NewsDto): List<NewsDto>

    @PATCH("news")
    suspend fun updateNews(
        @Query("id") id: String,
        @Body updates: Map<String, Any?>
    ): List<NewsDto>

    @DELETE("news")
    suspend fun deleteNews(
        @Query("id") id: String
    )
}
