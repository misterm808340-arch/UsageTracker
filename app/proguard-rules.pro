# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.usagetracker.app.**$$serializer { *; }
-keepclassmembers class com.usagetracker.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.usagetracker.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# WorkManager
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class * extends androidx.work.Worker { *; }

# Compose
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Keep app data models
-keep class com.usagetracker.app.data.local.entity.** { *; }
-keep class com.usagetracker.app.domain.model.** { *; }

# Google Mobile Ads / AdMob — keep public SDK classes (R8 already ships these rules;
# we keep them explicit so resource shrinking doesn't strip ad layouts/values).
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.ads.** { public *; }
-dontwarn com.google.android.gms.ads.**
-keepattributes Signature
-keepattributes InnerClasses

