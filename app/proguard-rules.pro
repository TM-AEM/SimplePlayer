# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Media3 ExoPlayer rules
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader & Decoders
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Moshi / Reflection / Data models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.example.data.** { *; }
-keep class com.example.player.** { *; }

# Jetpack DataStore Preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Skip multi-pass optimization for fast reproducible builds
-dontoptimize

