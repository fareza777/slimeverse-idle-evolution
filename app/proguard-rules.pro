# Slimeverse ProGuard rules
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.f7developer.slimeverse.**$$serializer { *; }
-keepclassmembers class com.f7developer.slimeverse.** { *** Companion; }
-keepclasseswithmembers class com.f7developer.slimeverse.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers @kotlinx.serialization.Serializable class com.f7developer.slimeverse.** { *; }

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**

# Play Billing
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# Compose
-dontwarn androidx.compose.**