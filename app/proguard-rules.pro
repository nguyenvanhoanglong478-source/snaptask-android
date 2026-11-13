# SnapTask ProGuard & R8 Optimization Rules for Low-End Devices

# Strip logging calls in release builds to save CPU and RAM
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Room Database v3 rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.snaptask.app.data.local.Converters { *; }
-keep class com.snaptask.app.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Keep Data Models, Enums & Preferences for reflection/serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class com.snaptask.app.domain.model.** { *; }
-keep class com.snaptask.app.data.preferences.** { *; }

# Jetpack Glance AppWidgets, Receivers & Action Callbacks
-keep class com.snaptask.app.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }

# BroadcastReceivers & Reminder Scheduler (Boot, Reminder, Notification Actions)
-keep class com.snaptask.app.reminder.** { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class com.snaptask.app.reminder.BootReceiver { *; }
-keep class com.snaptask.app.reminder.TaskReminderReceiver { *; }
-keep class com.snaptask.app.reminder.NotificationActionReceiver { *; }

# Coil Image loader rules
-dontwarn coil.**
-keep class coil.** { *; }
