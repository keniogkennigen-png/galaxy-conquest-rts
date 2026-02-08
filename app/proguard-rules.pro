# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep game entity classes
-keep class com.galaxycommand.rts.core.** { *; }
-keep class com.galaxycommand.rts.entities.** { *; }
-keep class com.galaxycommand.rts.factions.** { *; }
-keep class com.galaxycommand.rts.systems.** { *; }
-keep class com.galaxycommand.rts.ui.** { *; }

# Keep data classes for JSON serialization
-keepclassmembers class com.galaxycommand.rts.entities.** {
    <fields>;
    <init>(...);
}

# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# AndroidX rules
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Kotlin serialization
-keepattributes InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
