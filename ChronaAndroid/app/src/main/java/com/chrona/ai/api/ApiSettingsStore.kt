package com.chrona.ai.api

import android.content.Context

class ApiSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "chrona_api_settings",
        Context.MODE_PRIVATE
    )

    fun load(): ApiSettings {
        return ApiSettings(
            baseUrl = preferences.getString(KEY_BASE_URL, "") ?: "",
            apiKey = preferences.getString(KEY_API_KEY, "") ?: "",
            model = preferences.getString(KEY_MODEL, "") ?: ""
        )
    }

    fun save(settings: ApiSettings): Boolean {
        return preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_API_KEY, settings.apiKey.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .commit()
    }

    companion object {
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "api_model"
    }
}
