# Proguard rules for Rozkaam and Razorpay Checkout SDK
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** { *; }
-optimizations !method/inlining/*
-keepclasseswithmembers class * {
    public void onPaymentSuccess(java.lang.String);
    public void onPaymentError(int, java.lang.String);
}
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
