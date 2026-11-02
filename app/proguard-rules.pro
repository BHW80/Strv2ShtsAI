# Keep Kotlinx Serialization DTOs
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room generated DB & DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Retrofit API interfaces & DTOs
-keep class com.example.strava2sheets.data.remote.dto.** { *; }
-keep interface com.example.strava2sheets.data.remote.api.** { *; }
