# R8 configuration for Kotlin 2.3.10 compatibility
# Suppress Kotlin metadata parsing warnings

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }

# Dontwarn for Kotlin internal classes
-dontwarn kotlin.internal.**
-dontwarn kotlin.jvm.internal.**
-dontwarn kotlin.Metadata

# Allow R8 to process Kotlin metadata safely
-allowaccessmodification
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNull(java.lang.Object);
    static void checkNotNull(java.lang.Object, java.lang.String);
}

# Compose specific rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep annotation information
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
