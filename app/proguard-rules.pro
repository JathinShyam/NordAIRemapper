# Keep kotlinx.serialization serializers for backup models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.nordairemapper.** {
    *** Companion;
}
-keepclasseswithmembers class com.nordairemapper.** {
    kotlinx.serialization.KSerializer serializer(...);
}
