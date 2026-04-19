# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Wire
-keep class com.squareup.wire.** { *; }

# TransitKit models
-keep class com.transitkit.app.data.model.** { *; }
-keep class com.transitkit.app.config.** { *; }
