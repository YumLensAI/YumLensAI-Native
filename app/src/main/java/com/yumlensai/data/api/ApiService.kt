package com.yumlensai.data.api

import com.yumlensai.data.api.model.BenchmarkRecord
import com.yumlensai.data.api.model.RecipeObject
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @GET("health")
    suspend fun checkHealth()

    @Multipart
    @POST("predict")
    suspend fun predict(@Part image: MultipartBody.Part): List<String>

    @POST("recipes/search")
    suspend fun searchRecipes(@Body ingredients: List<String>): List<RecipeObject>

    @POST("benchmarks/local")
    suspend fun saveBenchmarkLocal(@Body records: List<BenchmarkRecord>)

    @POST("benchmarks/backend")
    suspend fun saveBenchmarkBackend(@Body records: List<BenchmarkRecord>)
}
