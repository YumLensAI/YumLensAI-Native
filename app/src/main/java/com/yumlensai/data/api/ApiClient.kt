package com.yumlensai.data.api

import android.content.Context
import com.yumlensai.data.storage.StorageManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private lateinit var appContext: Context
    private var cachedBaseUrl: String? = null
    private var cachedService: ApiService? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val service: ApiService
        get() {
            val currentUrl = StorageManager.getServerUrl(appContext)
            if (cachedService == null || cachedBaseUrl != currentUrl) {
                cachedBaseUrl = currentUrl
                cachedService = Retrofit.Builder()
                    .baseUrl(currentUrl.trimEnd('/') + "/")
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
            }
            return cachedService!!
        }
}
