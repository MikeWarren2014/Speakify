# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Clerk Rules ---
-keep class com.clerk.** { *; }
-keep interface com.clerk.** { *; }
-dontwarn com.clerk.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    java.lang.String getString(java.lang.String);
}

# --- General Serialization/Reflection ---
# If you use GSON or other serialization libraries, you might need more specific rules.
# For now, keeping your data models is a safe bet if they are used in networking.
-keep class com.mikewarren.speakify.data.** { *; }

# --- Firebase ---
# Firebase usually handles its own rules via the gradle plugin, but if you see issues:
#-keep class com.google.firebase.** { *; }
