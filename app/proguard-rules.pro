# 保留崩溃堆栈行号，隐藏源文件名（CMP 同款约定）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- kotlinx-serialization ----
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.lyf.composescaffold.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lyf.composescaffold.**$$serializer { *; }
-keepclassmembers class com.lyf.composescaffold.** {
    *** Companion;
}

# ---- Retrofit / OkHttp ----
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- MMKV（native 桥接，反射调用）----
-keep class com.tencent.mmkv.** { *; }
