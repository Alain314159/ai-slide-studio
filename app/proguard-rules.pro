# Keep Apache POI classes (used via reflection)
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.commons.**
-dontwarn org.etsi.**
-dontwarn org.w3.**

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep data models used in serialization
-keep class com.aislide.studio.data.remote.dto.** { *; }
-keep class com.aislide.studio.data.model.** { *; }

# Kotlin metadata
-keepattributes Kotlin.*
