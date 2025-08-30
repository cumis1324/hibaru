# =================================================================================
# ATURAN DASAR ANDROID & JAVA
# =================================================================================

# Aturan ini menjaga anotasi tetap ada saat runtime, penting untuk banyak library.
-keepattributes *Annotation*

# Menjaga semua kelas yang merupakan turunan dari android.app.Application.
-keep class com.theflexproject.thunder.MyApplication

# Menjaga semua kelas Activity, Service, dll., yang dideklarasikan di AndroidManifest.
# Ini penting agar sistem Android dapat menginisialisasi komponen-komponen ini.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

# Menjaga view kustom dan konstruktornya.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Menjaga semua kelas Parcelable dan creator-nya.
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Menjaga nama enum (misalnya untuk Enum.valueOf()).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =================================================================================
# FIREBASE & GOOGLE PLAY SERVICES
# =================================================================================

# Aturan umum untuk Firebase.
-keep class com.google.firebase.** { *; }
-keep class org.json.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.android.gms.ads.** { *; }

# Aturan untuk Firebase Auth, Database, Storage, dll.
# Diperlukan untuk menjaga kelas-kelas model yang digunakan dengan Firebase.
# Ganti 'com.theflexproject.thunder.model.**' jika model Anda ada di paket lain.
-keepnames class com.theflexproject.thunder.model.** { *; }
-keep class com.google.android.gms.auth.api.signin.internal.* { *; }


# =================================================================================
# GSON / JSON-SIMPLE / JACKSON (Sangat Penting!)
# =================================================================================

# Aturan ini menjaga SEMUA kelas di dalam paket 'model'.
# Ini mencegah R8 mengubah nama field di kelas POJO/Data Class Anda,
# yang akan merusak proses parsing JSON dari library seperti Gson dan Jackson.
-keep class com.theflexproject.thunder.model.** { *; }
-keep class com.theflexproject.thunder.model.TVShowInfo.** { *; }
-keep class com.theflexproject.thunder.model.tmdbImages.** { *; }
-keep class com.theflexproject.thunder.model.FanArt.** { *; }

# Menjaga anotasi yang digunakan oleh Gson.
-keep @com.google.gson.annotations.SerializedName class *

# Aturan spesifik untuk Jackson Databind
-keep class com.fasterxml.jackson.databind.** { *; }
-dontwarn com.fasterxml.jackson.databind.**


# =================================================================================
# ANDROIDX ROOM (Database)
# =================================================================================

# Menjaga semua entitas dan DAO (Data Access Object) Room.
# Ganti dengan path yang benar jika berbeda.
-keep class com.theflexproject.thunder.database.** { *; }
-keep class androidx.room.** { *; }


# =================================================================================
# GLIDE (Pustaka Gambar)
# =================================================================================

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType
-keepclassmembers class * {
    @com.bumptech.glide.annotation.GlideModule *;
}


# =================================================================================
# MEDIA3 / EXOPLAYER (Pemutar Video)
# =================================================================================

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**


# =================================================================================
# PUSTAKA PIHAK KETIGA LAINNYA
# =================================================================================

# Jsoup (HTML Parser)
-dontwarn org.jsoup.**

# FuzzyWuzzy (String Matching)
-keep class me.xdrop.fuzzywuzzy.** { *; }

# BlurView
-keep class eightbitlab.com.blurview.** { *; }

# ModernOnboarding
-keep class com.github.ErrorxCode.ModernOnboarding.** { *; }


# =================================================================================
# ATURAN TAMBAHAN (Pencegahan)
# =================================================================================

# Jika Anda menggunakan Serializable, aturan ini penting.
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