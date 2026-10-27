# SnapTask ProGuard & R8 Optimization Rules for Low-End Devices

# Strip logging calls in release builds to save CPU and RAM
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Data Models & Enums for reflection/serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class com.snaptask.app.domain.model.** { *; }
-keepclassmembers class com.snaptask.app.data.local.** { *; }

# Jetpack Glance Widget Action Callbacks
-keep public class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}

# Coil Image loader rules
-dontwarn coil.**
-keep class coil.** { *; }
