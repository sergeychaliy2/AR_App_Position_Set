# SceneView / Filament require JNI-exposed classes to be kept
-keep class com.google.android.filament.** { *; }
-keep class io.github.sceneview.** { *; }
-keep class com.google.ar.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.arpositionset.app.**$$serializer { *; }
-keepclassmembers class com.arpositionset.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.arpositionset.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Timber
-dontwarn org.jetbrains.annotations.**

# Keep data classes used with reflection
-keepclassmembers class com.arpositionset.app.domain.model.** { *; }
-keepclassmembers class com.arpositionset.app.data.remote.dto.** { *; }
