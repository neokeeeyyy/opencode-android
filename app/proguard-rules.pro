-keep class ai.opencode.android.data.model.** { *; }
-keep class ai.opencode.android.data.api.** { *; }

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class okio.** { *; }
-dontwarn okio.**
