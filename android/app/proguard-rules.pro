# ProGuard & R8 Kuralları - Master Racer

# 1. Annotations ve Reflection niteliklerini koru
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Veri modellerini (Firestore serialization/deserialization) koru
-keep class com.ecil.masterracer.data.model.** { *; }
-keepclassmembers class com.ecil.masterracer.data.model.** {
    <fields>;
    <methods>;
    <init>(...);
}

# 3. Firestore PropertyName anotasyonlu alan ve metodları koru
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# 4. Firebase Firestore ve Common kütüphaneleri
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
