package com.yumlensai

import android.app.Application
import com.yumlensai.data.api.ApiClient

class YumLensAIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
