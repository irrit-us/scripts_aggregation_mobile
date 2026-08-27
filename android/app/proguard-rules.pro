# Keep J2V8 classes
-keep class com.eclipsesource.v8.** { *; }
-dontwarn com.eclipsesource.v8.**

# Keep script model classes
-keep class com.scripthost.models.** { *; }

# Keep bridge interfaces
-keep class com.scripthost.bridge.** { *; }
-keep class com.scripthost.engine.ScriptBridge { *; }

# Keep engine classes whose methods J2V8 resolves by reflection
# (console.log, setTimeout, etc. are registered by name)
-keep class com.scripthost.engine.JavaScriptEngine { *; }

# Keep methods called from JavaScript
-keepclassmembers class com.scripthost.bridge.** {
    public *;
}

# Keep security classes
-keep class com.scripthost.security.** { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
