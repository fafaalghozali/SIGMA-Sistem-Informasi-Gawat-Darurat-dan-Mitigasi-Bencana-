package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.model.NewsDto
import com.mahasiswa.sigma.data.remote.api.SupabaseApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * News Repository using Retrofit for Supabase REST API
 */
@Singleton
class NewsRepositoryRetrofit @Inject constructor(
    private val supabaseApi: SupabaseApiService
) {

    companion object {
        private const val TAG = "NewsRepositoryRetrofit"
    }

    suspend fun getAllNews(): Result<List<NewsDto>> {
        return try {
            val news = supabaseApi.getNews()
            Log.d(TAG, "Fetched ${news.size} news items")
            Result.success(news)
        } catch (e: HttpException) {
            handleHttpException(e, "getAllNews")
        } catch (e: IOException) {
            handleNetworkError(e, "getAllNews")
        } catch (e: Exception) {
            handleGenericError(e, "getAllNews")
        }
    }

    suspend fun getNewsById(id: String): Result<NewsDto?> {
        return try {
            val news = supabaseApi.getNewsById(id = "eq.$id")
            Result.success(news.firstOrNull())
        } catch (e: HttpException) {
            handleHttpException(e, "getNewsById")
        } catch (e: IOException) {
            handleNetworkError(e, "getNewsById")
        } catch (e: Exception) {
            handleGenericError(e, "getNewsById")
        }
    }

    suspend fun createNews(news: NewsDto): Result<NewsDto> {
        return try {
            val newsList = supabaseApi.createNews(news)
            val newsItem = newsList.firstOrNull()
                ?: throw Exception("News creation did not return data")
            Log.d(TAG, "Created news: ${newsItem.title}")
            Result.success(newsItem)
        } catch (e: HttpException) {
            handleHttpException(e, "createNews")
        } catch (e: IOException) {
            handleNetworkError(e, "createNews")
        } catch (e: Exception) {
            handleGenericError(e, "createNews")
        }
    }

    suspend fun updateNews(id: String, updates: Map<String, Any?>): Result<NewsDto> {
        return try {
            val newsList = supabaseApi.updateNews(id = "eq.$id", updates = updates)
            val newsItem = newsList.firstOrNull()
                ?: throw Exception("News update did not return data")
            Log.d(TAG, "Updated news: ${newsItem.title}")
            Result.success(newsItem)
        } catch (e: HttpException) {
            handleHttpException(e, "updateNews")
        } catch (e: IOException) {
            handleNetworkError(e, "updateNews")
        } catch (e: Exception) {
            handleGenericError(e, "updateNews")
        }
    }

    suspend fun deleteNews(id: String): Result<Unit> {
        return try {
            supabaseApi.deleteNews(id = "eq.$id")
            Log.d(TAG, "Deleted news: $id")
            Result.success(Unit)
        } catch (e: HttpException) {
            handleHttpException(e, "deleteNews")
        } catch (e: IOException) {
            handleNetworkError(e, "deleteNews")
        } catch (e: Exception) {
            handleGenericError(e, "deleteNews")
        }
    }

    // ==================== ERROR HANDLING ====================

    private fun <T> handleHttpException(e: HttpException, operation: String): Result<T> {
        val errorMessage = when (e.code()) {
            400 -> "Bad request: Invalid data"
            401 -> "Unauthorized: Invalid API key or token"
            403 -> "Forbidden: Insufficient permissions"
            404 -> "Not found"
            409 -> "Conflict: Resource already exists"
            422 -> "Unprocessable entity: Validation failed"
            else -> "HTTP error: ${e.message()}"
        }
        Log.e(TAG, "$operation HttpException: $errorMessage", e)
        return Result.failure(Exception(errorMessage))
    }

    private fun <T> handleNetworkError(e: IOException, operation: String): Result<T> {
        Log.e(TAG, "$operation IOException: Network error", e)
        return Result.failure(Exception("Network error: Please check your internet connection"))
    }

    private fun <T> handleGenericError(e: Exception, operation: String): Result<T> {
        Log.e(TAG, "$operation Exception: ${e.message}", e)
        return Result.failure(e)
    }
}
