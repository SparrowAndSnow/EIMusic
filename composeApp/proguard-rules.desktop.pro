-dontwarn com.sun.jna.internal.Cleaner
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# LayoutHitTestOwner ProGuard rules
-keep class androidx.compose.foundation.HoverableNode { *; }
-keep class androidx.compose.foundation.gestures.ScrollableNode { *; }

-keep class androidx.compose.ui.scene.PlatformLayersComposeSceneImpl { *; }
-keep class androidx.compose.ui.scene.CanvasLayersComposeSceneImpl { *; }
-keep class androidx.compose.ui.scene.CanvasLayersComposeSceneImpl$AttachedComposeSceneLayer { *; }

-keepclassmembers class androidx.compose.ui.scene.PlatformLayersComposeSceneImpl {
    private *** getMainOwner();
}

-keepclassmembers class androidx.compose.ui.scene.CanvasLayersComposeSceneImpl {
    private *** mainOwner;
    private *** _layersCopyCache;
    private *** focusedLayer;
}

-keepclassmembers class androidx.compose.ui.scene.CanvasLayersComposeSceneImpl$AttachedComposeSceneLayer {
    private *** owner;
    private *** isInBounds(...);
}

# Fix for Ktor Network Sockets
-dontwarn io.ktor.network.sockets.SocketBase$attachFor$1
-keep class io.ktor.** { *; }

# Fix for kotlinx.io classes
-keep class kotlinx.io.** { *; }
-dontwarn kotlinx.io.**

# Fix for JavaFX classes
-keep class com.sun.javafx.** { *; }
-keep class javafx.** { *; }
-keep class com.sun.glass.** { *; }
-keep class com.sun.scenario.** { *; }
-keep class com.sun.prism.** { *; }
-keep class com.sun.marlin.** { *; }
-keep class com.sun.pisces.** { *; }
-keep class com.sun.media.jfxmedia.** { *; }
-keep class com.sun.media.jfxmedia.locator.Locator { *; }
-dontwarn com.sun.media.jfxmedia.**
-dontwarn com.sun.glass.**
-dontwarn com.sun.prism.**
-dontwarn com.sun.marlin.**
-dontwarn com.sun.pisces.**
-dontwarn com.sun.scenario.**