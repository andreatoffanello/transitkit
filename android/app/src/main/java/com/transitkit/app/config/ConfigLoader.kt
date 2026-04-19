package com.transitkit.app.config

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object ConfigLoader {
    fun load(context: Context): OperatorConfig {
        val json = context.assets.open("config.json").bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        return moshi.adapter(OperatorConfig::class.java).fromJson(json)
            ?: error("Failed to parse config.json")
    }
}
