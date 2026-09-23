# ==========================================
# JAVAFX
# ==========================================
-keep class javafx.** { *; }
-keep interface javafx.** { *; }
-keep enum javafx.** { *; }
-keep class javafx.base.** { *; }
-keep interface javafx.base.** { *; }
-keep class javafx.beans.** { *; }
-keep interface javafx.beans.** { *; }
-keep class javafx.beans.value.** { *; }
-keep interface javafx.beans.value.** { *; }
-keep class javafx.collections.** { *; }
-keep interface javafx.collections.** { *; }
-keep class javafx.animation.** { *; }
-keep interface javafx.animation.** { *; }
-keep class javafx.application.** { *; }
-keep interface javafx.application.** { *; }
-keep class javafx.scene.** { *; }
-keep interface javafx.scene.** { *; }
-keep class javafx.stage.** { *; }
-keep interface javafx.stage.** { *; }
-keep class javafx.util.** { *; }
-keep interface javafx.util.** { *; }
-keep class javafx.media.** { *; }
-keep interface javafx.media.** { *; }
-keep class javafx.graphics.** { *; }
-keep interface javafx.graphics.** { *; }

-keep class com.sun.javafx.** { *; }
-keep interface com.sun.javafx.** { *; }
-keep class com.sun.media.jfxmedia.** { *; }
-keep interface com.sun.media.jfxmedia.** { *; }
-keep class com.sun.media.jfxmediaimpl.** { *; }
-keep interface com.sun.media.jfxmediaimpl.** { *; }
-keep class com.sun.glass.** { *; }
-keep interface com.sun.glass.** { *; }
-keep class com.sun.prism.** { *; }
-keep interface com.sun.prism.** { *; }
-keep class com.sun.scenario.** { *; }
-keep interface com.sun.scenario.** { *; }

-keep class com.sun.deploy.** { *; }
-keep class com.sun.fx.** { *; }
-keep class com.sun.java.browser.** { *; }
-keep class com.sun.webkit.** { *; }
-keep class javafx.application.Platform { *; }

-keepclassmembers class javafx.** {
    public *;
    protected *;
}

-keepclassmembers class com.sun.javafx.** {
    public *;
    protected *;
}

-keepclassmembers class com.sun.media.** {
    public *;
    protected *;
}

-dontwarn javafx.**
-dontwarn com.sun.javafx.**
-dontwarn com.sun.media.jfxmedia.**