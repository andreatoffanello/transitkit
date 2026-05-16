package com.transitkit.app.data.push

import android.os.Build
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.transitkit.app.config.OperatorConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the transitkit-console CMS for public endpoints — currently just
 * the test-device registration. The base URL comes from
 * `OperatorConfig.consoleApiUrl`. In debug builds we override to
 * `http://10.0.2.2:3000` (the Android emulator's loopback to the host) so
 * dev smoke tests against a locally running CMS work out of the box.
 */
@Singleton
class PushApiClient @Inject constructor(
    private val config: OperatorConfig,
) {
    private val http = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(RegisterRequest::class.java)

    @JsonClass(generateAdapter = true)
    data class RegisterRequest(
        val operator_id: String,
        val fcm_token: String,
        val platform: String,
        val label: String,
    )

    sealed class Result {
        data class Ok(val id: String?) : Result()
        data class Err(val message: String) : Result()
    }

    /** Resolve the CMS base URL.
     *  - Debug build:   `http://10.0.2.2:3000` (emulator → host loopback)
     *  - Release build: the operator's `console_api_url` from config.json */
    fun resolveBaseUrl(): String? {
        if (com.transitkit.app.BuildConfig.DEBUG) {
            return "http://10.0.2.2:3000"
        }
        return config.consoleApiUrl?.takeIf { it.isNotBlank() }
    }

    fun defaultDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val display = if (model.startsWith(manufacturer, ignoreCase = true)) model
                      else "$manufacturer $model"
        return "$display — Android ${Build.VERSION.RELEASE}"
    }

    suspend fun registerTestDevice(token: String, label: String): Result {
        val base = resolveBaseUrl()?.trimEnd('/')
            ?: return Result.Err("console_api_url missing from operator config")
        val url = "$base/api/test-devices/register"

        val body = adapter.toJson(
            RegisterRequest(
                operator_id = config.id,
                fcm_token = token,
                platform = "android",
                label = label,
            )
        ).toRequestBody("application/json".toMediaType())

        return try {
            val response = http.newCall(
                Request.Builder().url(url).post(body).build()
            ).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    Result.Ok(resp.body?.string())
                } else {
                    Result.Err("HTTP ${resp.code}: ${resp.body?.string()?.take(200) ?: ""}")
                }
            }
        } catch (e: Exception) {
            Result.Err("Network error: ${e.message}")
        }
    }
}
