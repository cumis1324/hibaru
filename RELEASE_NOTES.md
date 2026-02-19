# NFGPlus Release Notes

## 📱 Version: Ekasthadasa (v81)
**Release Date:** February 19, 2026  
**Minimum SDK:** Android 7.0 (API 24)  
**Target SDK:** Android 16 (API 36)

---

## 🎯 Overview

**NFGPlus** adalah aplikasi streaming film dan serial TV modern untuk Android yang menggunakan arsitektur **Offline-First** dengan sinkronisasi data real-time dari backend Cloudflare. Aplikasi ini mendukung pengalaman tanpa gangguan dengan database lokal prepopulasi dan sync inkremental yang efisien.

---

## ✨ Fitur Utama

### 🔄 Sinkronisasi Data Hybrid (Smart Sync)
- **Offline-First Architecture**: Menggunakan Room Database lokal dengan prepopulasi data
- **Delta Sync**: Hanya mengambil data yang berubah sejak last sync timestamp
- **Background Sync**: SyncWorker otomatis menjaga data tetap fresh setiap 15 menit
- **Dual Database**: Dukungan Production DB dan Demo DB untuk testing

### 📺 Streaming Media
- **Native Video Player**: Custom UI berbasis Media3/ExoPlayer untuk playback high-performance
- **Multi-Format Support**: Streaming dari berbagai sumber (Google Drive via GDIndex)
- **Quality Adaptation**: Penyesuaian otomatis kualitas video berdasarkan koneksi internet
- **Subtitle Support**: Subtitle terintegrasi dengan kontrol playback lengkap

### 🎭 Dukungan Konten Lengkap
- **Movies**: Database lengkap film dengan metadata dari TMDB
- **TV Shows**: Dukungan penuh untuk Seasons dan Episodes
- **Metadata Rich**: Informasi lengkap termasuk cast, crew, ratings, dan rekomendasi

### 🔍 Search & Discovery
- **Local Search**: Pencarian cepat berbasis database lokal
- **Advanced Filtering**: Filter berdasarkan genre, tahun, popularitas, rating
- **Personalized Recommendations**: Rekomendasi berdasarkan history dan preferensi

### 👤 Fitur Personalisasi
- **Firebase Authentication**: Login/Sign Up dengan Firebase
- **Watchlist Personal**: Daftar tontonan pribadi yang tersimpan
- **Watch History**: Riwayat film yang telah ditonton
- **Progress Tracking**: Melanjutkan dari mana Anda berhenti

### 📺 Dukungan TV Device (Android TV/Leanback)
- **Responsive UI**: Layout adaptif untuk TV dan phone
- **D-Pad Navigation**: Kontrol lengkap dengan D-pad/remote
- **TV Provider Integration**: Home Screen integration dan recommendations
- **Channel Management**: Manajemen channel dan EPG

### 💰 Monetisasi & Premium
- **Unity Ads Integration**: Sistem iklan terintegrasi
- **Premium Subscription**: Layanan tanpa iklan untuk pengguna premium
- **Payment System**: Integrasi dengan sistem pembayaran

### 🔐 Keamanan & Privacy
- **Firebase Security**: Autentikasi aman dengan Firebase
- **Data Encryption**: Enkripsi data dalam transit dan storage lokal
- **Permission Management**: Runtime permission handling yang proper
- **ProGuard/R8 Obfuscation**: Code protection di release build

---

## 🏗️ Arsitektur & Teknologi

### **Android Client (100% Kotlin)**
```
Platform:          Android 7.0+ (API 24-36)
Language:          Kotlin 1.9.22
Compiler:          JVM Target 17
```

**Dependencies Utama:**
- **DI**: Hilt 2.50 (Dagger)
- **Async**: Coroutines 1.7.3 + Flow
- **Database**: Room + SQLite (AppDatabase)
- **Network**: Retrofit 2.9.0 + OkHttp 4.12.0
- **JSON**: Moshi 1.15.0
- **Video**: Media3/ExoPlayer
- **Background**: WorkManager
- **Analytics**: Firebase Analytics
- **UI**: Material Design 3 + Compose 1.5.8

### **Backend (Cloudflare Workers)**
```
Platform:          Cloudflare Workers
Language:          TypeScript 5.3.3
Database:          Cloudflare D1 (Serverless SQLite)
```

**Database:**
- Production DB (`nfgplus-db`)
- Demo DB (`nfgplus-demo-db`) untuk testing

**API Endpoints:**
- `GET /api/movies` - Fetch movies
- `GET /api/tvshows` - Fetch TV shows
- `GET /api/episodes` - Fetch episodes
- `GET /api/seasons` - Fetch seasons
- `GET /api/genres` - Fetch genres
- `POST /api/admin/gdids` - Admin GDIndex management
- `GET /api/health` - Health check

### **Data Model**
```
📦 AppDatabase (Room)
├── Movie (dengan full-text search)
├── TVShowInfo (series metadata)
├── TVShowSeasonDetails (season info)
├── Episode (episode details)
├── IndexLinks (Google Drive links)
├── ResFormat (resolution format)
├── Cast & Crew
└── GenreInfo (genre classification)
```

### **Automation Pipeline**
- **GitHub Actions**: Scheduled workflows untuk indexing GDIndex
- **Python Scripts**: Import tools dengan TMDB matching
- **Sequential Scanning**: Movies → TV Series prioritization

---

## 🔧 Perbaikan & Improvement dalam Versi Ini

### 🐛 Bug Fixes

#### ✅ Unity Ads Permission Error (CRITICAL FIX)
**Problem**: Error di release build - "Unity Ads was not able to get current network type due to missing permission"
- **Root Cause**: ProGuard/R8 menghapus ConnectivityMonitor class yang digunakan Unity Ads via reflection
- **Solusi**: 
  - Ditambahkan comprehensive ProGuard rules untuk Unity Ads
  - Melindungi ConnectivityMonitor dan reflection-based code
  - Menambahkan rules untuk listeners dan callbacks
  - Preserving enum dan interface yang penting
  
**ProGuard Rules Added:**
```proguard
-keep class com.unity3d.services.core.connectivity.ConnectivityMonitor { *; }
-keep class com.unity3d.services.core.connectivity.** { *; }
-keepclassmembers class com.unity3d.services.core.connectivity.** {
    *** *(...);
}
```

#### ✅ Permission Handling di Release Build
**Improvement**: Moved `checkPermissions()` function dari SyncActivity ke MainActivity
- Runtime permission request yang lebih tepat
- Permission check dilakukan saat app start
- Better UX untuk Android 6.0+ devices

### 🎨 UI/UX Improvements
- Edge-to-Edge rendering untuk modern Android devices
- Improved window insets handling
- Better navigation between screens
- Enhanced visibility logic untuk berbagai layouts

### ⚡ Performance Optimizations
- Delta sync mengurangi bandwidth usage
- Local full-text search untuk faster queries
- Optimized database indexes
- Memory-efficient async operations

### 🔒 Security Enhancements
- Improved ProGuard rules untuk code protection
- Better API key management via headers
- CORS configuration untuk backend API
- Runtime permission validation

---

## 📋 System Requirements

### **Minimum Requirements**
- **OS**: Android 7.0 (API 24) or higher
- **RAM**: 2GB minimum (4GB recommended)
- **Storage**: 200MB free space
- **Internet**: Stable connection required for streaming

### **Recommended Requirements**
- **OS**: Android 10+ (API 29+)
- **RAM**: 4GB or more
- **Storage**: 500MB free space
- **Network**: 4G/WiFi with 5+ Mbps for HD streaming

### **Optional**
- **TV Device**: Android TV for enhanced experience
- **Remote Control**: For TV device navigation

---

## 📥 Installation & Setup

### **From APK**
```bash
adb install app-release.apk
```

### **From Source**
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build release Bundle (Play Store)
./gradlew bundleRelease
```

---

## 🚀 Key Changes Summary

| Kategori | Perubahan |
|----------|-----------|
| **Bug Fixes** | Unity Ads permission error di release build |
| **Code Quality** | Comprehensive ProGuard rules, better permission handling |
| **Architecture** | Moved permission check ke MainActivity |
| **Performance** | Optimized sync dan database queries |
| **Security** | Enhanced code protection dengan R8/ProGuard |

---

## 📊 Database Schema

### **Movies Table**
```sql
CREATE TABLE Movie (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    releaseDate TEXT,
    rating REAL,
    posterUrl TEXT,
    backdropUrl TEXT,
    tmdbId INTEGER UNIQUE,
    lastUpdated INTEGER
);
```

### **TV Shows Table**
```sql
CREATE TABLE TVShowInfo (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    firstAirDate TEXT,
    rating REAL,
    posterUrl TEXT,
    tmdbId INTEGER UNIQUE,
    seasonCount INTEGER,
    lastUpdated INTEGER
);
```

### **Episodes Table**
```sql
CREATE TABLE Episode (
    id INTEGER PRIMARY KEY,
    showId INTEGER,
    seasonNumber INTEGER,
    episodeNumber INTEGER,
    title TEXT,
    description TEXT,
    airDate TEXT,
    stillUrl TEXT,
    FOREIGN KEY(showId) REFERENCES TVShowInfo(id)
);
```

---

## 🧪 Testing

### **Automated Tests**
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### **Manual Testing**
```powershell
# Fresh install & test
.\test-app.ps1 test-fresh

# Quick rebuild & test
.\test-app.ps1 build
.\test-app.ps1 install

# View sync logs
.\test-app.ps1 logs
```

### **Test Scenarios**
- ✅ First-time sync
- ✅ Delta sync (incremental updates)
- ✅ Offline mode (local database queries)
- ✅ Permission requests
- ✅ Deep link handling
- ✅ TV device navigation
- ✅ Ads display & interaction
- ✅ Playback resume

---

## 🔄 Update Cycle

### **Sync Schedule**
- **Data Sync**: Every 15 minutes via WorkManager
- **TV Channel Sync**: Every 24 hours
- **Engage SDK Sync**: Every 24 hours
- **Manual Trigger**: Available on demand

### **Content Updates**
- **GitHub Actions**: 4 times daily automatic GDIndex scanning
- **Sequential Processing**: Movies → TV Shows priority
- **Admin Trigger**: Manual scan available via admin panel

---

## 🎓 Developer Notes

### **Key Classes**
| Class | Purpose |
|-------|---------|
| `MainActivity` | Main screen dengan navigation & permission check |
| `SyncActivity` | Loading screen & data synchronization |
| `AppDatabase` | Room database dengan semua DAOs |
| `SyncManager` | Orchestrates all sync operations |
| `SyncWorker` | Background periodic sync task |
| `MovieRepository` | Data layer untuk movies |
| `TVShowRepository` | Data layer untuk TV shows |

### **Important Files**
```
├── proguard-rules.pro (ProGuard configuration dengan Unity Ads rules)
├── AndroidManifest.xml (Permissions & components declaration)
├── MainActivity.kt (Permission checking & navigation)
├── SyncActivity.kt (Loading & sync orchestration)
└── MyApplication.kt (App initialization & WorkManager setup)
```

### **Build Configuration**
- **Compile SDK**: 36
- **Min SDK**: 24
- **Target SDK**: 36
- **Java Version**: 17
- **Kotlin Version**: 1.9.22

---

## ⚠️ Known Issues & Limitations

### **Known Issues**
None currently reported for this release.

### **Limitations**
- Streaming quality depends on internet connection speed
- TV device support requires Android TV OS
- ProGuard/R8 may affect some debug features in release build

---

## 🚀 Future Roadmap

### **Planned Features**
- [ ] Download for offline viewing
- [ ] Advanced subtitle management
- [ ] Picture-in-Picture mode enhancement
- [ ] Push notifications for new releases
- [ ] Social features (ratings, reviews)
- [ ] Multi-language support

### **Performance Improvements**
- [ ] Implement caching strategy optimization
- [ ] Database query optimization
- [ ] Network request batching

---

## 📞 Support & Feedback

### **Issue Reporting**
Untuk melaporkan bug atau masalah:
1. Capture logs menggunakan `adb logcat`
2. Catat exact reproduction steps
3. Lapor dengan device model dan Android version

### **Performance Tips**
- Pastikan minimum 200MB free storage
- Update app secara berkala untuk fixes terbaru
- Clear app cache jika mengalami lag
- Check internet connection untuk streaming issues

---

## 📄 License

NFGPlus is released under MIT License.

---

## 🎉 Credits

**Development Team:**
- Architecture & Backend: Cloudflare Workers + D1
- Android Client: Kotlin + Hilt
- Content Integration: TMDB + GDIndex

**Technologies Used:**
- Firebase (Authentication, Analytics, Messaging)
- Cloudflare Workers & D1
- Media3/ExoPlayer
- Room Database
- Retrofit + OkHttp
- Hilt/Dagger DI

---

**Last Updated:** February 19, 2026  
**Next Release Target:** Q1 2026

---

## 📊 Changelog Summary

### v81 (Ekasthadasa) - Current Release
- ✅ Fixed Unity Ads permission error in release build
- ✅ Improved ProGuard rules untuk better code protection
- ✅ Refactored permission checking ke MainActivity
- ✅ Enhanced error handling dan logging
- ✅ Updated documentation & testing guides

---

## 🔐 Security Checklist

- ✅ Firebase authentication enabled
- ✅ API key protected via header authentication
- ✅ HTTPS/TLS for all API calls
- ✅ ProGuard/R8 code obfuscation
- ✅ Runtime permission handling
- ✅ Secure local database storage
- ✅ CORS configured properly

---

**Version:** Ekasthadasa (81)  
**Build Date:** 2026-02-19  
**Status:** ✅ Stable Release

