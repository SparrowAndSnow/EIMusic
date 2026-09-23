-dontwarn kotlinx.datetime.**
-dontwarn org.slf4j.**
-keep class org.slf4j.**{ *; }

# 保护 Navigation3 内部逻辑
-keep class androidx.navigation3.** { *; }
-keep class androidx.navigationevent.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}