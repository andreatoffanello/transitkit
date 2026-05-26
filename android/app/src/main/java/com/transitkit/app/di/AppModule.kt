package com.transitkit.app.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.api.TransitApiService
import com.transitkit.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOperatorConfig(
        @ApplicationContext context: Context,
        moshi: Moshi,
    ): OperatorConfig {
        val json = context.assets.open("config.json").bufferedReader().use { it.readText() }
        val adapter = moshi.adapter(OperatorConfig::class.java)
        return requireNotNull(adapter.fromJson(json)) { "Failed to parse config.json" }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(false)
            .addInterceptor(HttpsUpgradeInterceptor())
            .addInterceptor(RetryInterceptor())
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    // Follows redirects manually, upgrading http:// → https:// to avoid cleartext blocks.
    // Needed because GitHub Pages custom domain redirects return http:// Location headers.
    private class HttpsUpgradeInterceptor(private val maxHops: Int = 5) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            var request = chain.request()
            var hops = 0
            while (hops++ < maxHops) {
                val response = chain.proceed(request)
                if (!response.isRedirect) return response
                val location = response.header("Location") ?: return response
                response.close()
                val httpsLocation = if (location.startsWith("http://")) {
                    "https://" + location.removePrefix("http://")
                } else {
                    location
                }
                request = request.newBuilder().url(httpsLocation).build()
            }
            return chain.proceed(request)
        }
    }

    private class RetryInterceptor(private val maxRetries: Int = 2) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            var lastException: Exception? = null
            repeat(maxRetries + 1) { attempt ->
                try {
                    val response = chain.proceed(chain.request())
                    if (response.isSuccessful || attempt == maxRetries) return response
                    response.close()
                } catch (e: Exception) {
                    lastException = e
                    if (attempt == maxRetries) throw e
                }
            }
            throw lastException ?: IllegalStateException("Retry failed")
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        config: OperatorConfig,
    ): Retrofit {
        // Derive API base URL from cdnUrl or fall back to a reasonable default
        val baseUrl = config.cdnUrl?.let { cdn ->
            if (cdn.endsWith("/")) cdn else "$cdn/"
        } ?: "https://api.transitkit.app/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideTransitApiService(retrofit: Retrofit): TransitApiService =
        retrofit.create(TransitApiService::class.java)

}
