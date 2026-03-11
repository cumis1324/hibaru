# =================================================================================
# ATURAN DASAR ANDROID & JAVA
# =================================================================================

# Menjaga anotasi penting
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Kelas dasar Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

# View Kustom
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =================================================================================
# HILT / DAGGER (DI - PENTING)
# =================================================================================
-keep class com.theflexproject.thunder.Hilt_** { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.EntryPoint <init>(...);
}
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*

# =================================================================================
# KOTLIN COROUTINES
# =================================================================================
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# =================================================================================
# FIREBASE & GOOGLE SERVICES
# =================================================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# =================================================================================
# NETWORK (OKHTTP, RETROFIT, MOSHI, GSON)
# =================================================================================
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson & Models (PENTING UNTUK PARSING)
-keep class com.theflexproject.thunder.model.** { *; }
-keep class com.theflexproject.thunder.model.TVShowInfo.** { *; }
-keep class com.theflexproject.thunder.model.tmdbImages.** { *; }
-keep class com.theflexproject.thunder.model.FanArt.** { *; }
# Mencegah R8 menghapus field yang dipakai Gson/Moshi melalui reflection
-keepclassmembers class com.theflexproject.thunder.model.** { <fields>; }

# Network DTOs (NFGPlus & TMDB API)
-keep class com.theflexproject.thunder.network.** { *; }
-keep class com.theflexproject.thunder.network.dto.** { *; }
-keepclassmembers class com.theflexproject.thunder.network.** { <fields>; }
-keepclassmembers class com.theflexproject.thunder.network.dto.** { <fields>; }

-keep @com.google.gson.annotations.SerializedName class *

# Moshi Kotlin Reflection (Penting untuk KotlinJsonAdapterFactory)
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# Jackson
-keep class com.fasterxml.jackson.databind.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# =================================================================================
# GAMBAR (GLIDE)
# =================================================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType
-keepclassmembers class * {
    @com.bumptech.glide.annotation.GlideModule *;
}

# =================================================================================
# DATABASE (ROOM)
# =================================================================================
-keep class androidx.room.** { *; }
-keep class com.theflexproject.thunder.database.** { *; }
-keepnames class com.theflexproject.thunder.database.** { *; }
-dontwarn androidx.room.paging.**

# =================================================================================
# MEDIA (LIBVLC - NO MINIFY)
# =================================================================================
-keep class org.videolan.libvlc.** { *; }
-keep interface org.videolan.libvlc.** { *; }
-keep class org.videolan.vlc.** { *; }
-keep interface org.videolan.vlc.** { *; }
-dontwarn org.videolan.libvlc.**
-dontwarn org.videolan.vlc.**

# Keep all native methods and members for VLC
-keepclassmembers class org.videolan.libvlc.** { *; }
-keepclassmembers class org.videolan.vlc.** { *; }

# PENTING: Jangan biarkan R8 mengganti nama method onEvent atau method listener lainnya
# yang dipanggil dari Native VLC melalui JNI.
-keep class * implements org.videolan.libvlc.interfaces.I** { *; }
-keep class * implements org.videolan.libvlc.MediaPlayer$EventListener { *; }
-keepclassmembers class * implements org.videolan.libvlc.MediaPlayer$EventListener {
    void onEvent(org.videolan.libvlc.MediaPlayer$Event);
}

-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Google Engage SDK & TV Provider (Integration for Android TV)
-keep class com.google.android.engage.** { *; }
-keep class androidx.tvprovider.** { *; }

# WorkManager (Background Sync)
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# =================================================================================
# IKLAN (UNITY ADS) - PENTING SETELAH PERBAIKAN
# =================================================================================
-keep class com.unity3d.ads.** { *; }
-keep interface com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-keep interface com.unity3d.services.** { *; }

# Unity Ads Connectivity Monitor - Untuk mengatasi error "missing permission"
-keep class com.unity3d.services.core.connectivity.ConnectivityMonitor { *; }
-keep class com.unity3d.services.core.connectivity.** { *; }
-keepclassmembers class com.unity3d.services.core.connectivity.** {
    *** *(...);
}

# Unity Ads Banner & Rewarded
-keep class com.unity3d.services.banners.** { *; }
-keep class com.unity3d.services.rewarded.** { *; }
-keepclassmembers class com.unity3d.services.banners.** {
    *** *(...);
}

# Unity Ads Listeners & Callbacks
-keep class com.unity3d.ads.IUnityAdsInitializationListener { *; }
-keep class com.unity3d.ads.IUnityAdsLoadListener { *; }
-keep class com.unity3d.ads.IUnityAdsShowListener { *; }
-keep interface com.unity3d.ads.IUnityAdsInitializationListener { *; }
-keep interface com.unity3d.ads.IUnityAdsLoadListener { *; }
-keep interface com.unity3d.ads.IUnityAdsShowListener { *; }

# Jangan obfuscate Unity Ads enums
-keepclassmembers class com.unity3d.ads.UnityAds {
    public static ** UnityAdsShowCompletionState;
    public static ** UnityAdsLoadError;
    public static ** UnityAdsShowError;
    public static ** UnityAdsInitializationError;
}
-keepclassmembers enum com.unity3d.ads.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =================================================================================
# LAIN-LAIN
# =================================================================================
# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# BlurView
-keep class eightbitlab.com.blurview.** { *; }

# FuzzyWuzzy
-keep class me.xdrop.fuzzywuzzy.** { *; }

# ModernOnboarding
-keep class com.github.ErrorxCode.ModernOnboarding.** { *; }

# Billing Client (In-App Purchases)
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# json-simple
-keep class org.json.simple.** { *; }
-dontwarn org.json.simple.**

# Apache Commons Text
-keep class org.apache.commons.text.** { *; }
-dontwarn org.apache.commons.**

# Guava / Google Common Reflect (Fix R8 Missing Class AnnotatedType)
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn com.google.common.reflect.**

# Joda-Time: joda-convert adalah optional dependency yang tidak disertakan di Android
# R8 melaporkannya sebagai missing class, tapi tidak dipakai saat runtime di Android
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# sun.misc.Unsafe: hanya ada di JVM desktop, tidak di Android runtime (normal)
-dontwarn sun.misc.Unsafe
