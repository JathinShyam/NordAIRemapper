# Keep kotlinx.serialization serializers for backup models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.nordairemapper.** {
    *** Companion;
}
-keepclasseswithmembers class com.nordairemapper.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# In-app Wireless ADB (libadb-android + Conscrypt + sun-security-android)
-keep class io.github.muntashirakon.adb.** { *; }
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**
-dontwarn sun.security.**
-dontwarn android.sun.security.**
-keep class sun.security.** { *; }
-keep class android.sun.security.** { *; }
