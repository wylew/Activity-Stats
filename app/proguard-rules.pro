# kotlinx.serialization: keep generated serializers for our DTOs and models.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.wylew.activitystats.** {
    *** Companion;
}
-keepclasseswithmembers class com.wylew.activitystats.** {
    kotlinx.serialization.KSerializer serializer(...);
}
