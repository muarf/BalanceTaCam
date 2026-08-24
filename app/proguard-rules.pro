# Add project specific ProGuard rules here.

# Keep data models
-keep class com.osmcamera.mapper.data.model.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# OSMDroid
-dontwarn org.osmdroid.**

# ScribeJava OAuth
-keep class com.github.scribejava.** { *; }
-dontwarn com.github.scribejava.**

# DataStore : le protobuf embarqué est casse par R8 sinon (ClassCastException au demarrage)
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.preferences.protobuf.** { *; }
-dontwarn androidx.datastore.preferences.protobuf.**


