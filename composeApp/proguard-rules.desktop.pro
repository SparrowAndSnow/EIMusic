# ==========================================
# JNA
# ==========================================
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
-keep class * extends com.sun.jna.Structure { *; }
-keep class * extends com.sun.jna.PointerType { *; }
-keep class * implements com.sun.jna.Library { *; }

# ==========================================
# Ktor
# ==========================================
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.cio.CIOEngineContainer { *; }
-keep class io.ktor.client.HttpClientEngineContainer { *; }
-keep class io.ktor.network.selector.InterestSuspensionsMap { *; }

-dontwarn okio.**
-keep class okio.** { *; }
-keep interface okio.** { *; }

# ==========================================
# Coil
# ==========================================
-dontwarn coil3.**
-keep class coil3.util.DecoderServiceLoaderTarget { *; }
-keep class coil3.util.FetcherServiceLoaderTarget { *; }
-keep class coil3.util.ServiceLoaderComponentRegistry { *; }
-keep class coil3.PlatformContext { *; }
-keep class * implements coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * implements coil3.util.FetcherServiceLoaderTarget { *; }

# ==========================================
# 忽略警告
# ==========================================
-dontwarn com.sun.glass.**
-dontwarn com.sun.jna.**
-dontwarn META-INF.**
-dontwarn com.sun.scenario.**
-dontwarn sun.misc.**
-dontwarn java.awt.**

