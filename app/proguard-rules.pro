# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ARCore
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**
