# Add project specific ProGuard rules here.
# Keep Ktor serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep model classes
-keep class com.dustinmcafee.dongadeuce.models.** { *; }
-keep class com.dustinmcafee.dongadeuce.network.** { *; }
