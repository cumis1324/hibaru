# NFGPlus - Aplikasi Streaming Film dan Serial TV

## Apa Fungsi dari Aplikasi Ini?

**NFGPlus** adalah aplikasi streaming Android yang memungkinkan pengguna untuk menonton film dan serial TV secara online. Aplikasi ini menyediakan platform yang lengkap untuk streaming media dengan berbagai fitur canggih.

## Fitur Utama

### 🎬 **Streaming Film dan Serial TV**
- Menonton film terbaru dan populer
- Streaming serial TV dengan episode lengkap
- Dukungan kualitas video yang dapat disesuaikan
- Player video yang mendukung berbagai format

### 📚 **Perpustakaan Media Lengkap**
- Katalog film yang luas dari berbagai genre
- Koleksi serial TV termasuk drakor (drama Korea)
- Film Indonesia dan konten lokal
- Sistem pencarian dan filter yang mudah digunakan

### 🔍 **Fitur Pencarian dan Rekomendasi**
- Pencarian film dan serial berdasarkan judul
- Rekomendasi berdasarkan rating dan popularitas
- Kategori trending mingguan
- Film dan serial yang baru ditambahkan

### 👤 **Manajemen Akun Pengguna**
- Sistem login dengan Firebase Authentication
- Profil pengguna yang dapat dikustomisasi
- Sinkronisasi data antar perangkat
- Riwayat tontonan

### 📱 **Fitur Premium**
- Langganan premium untuk akses tanpa iklan
- Integrasi dengan sistem pembayaran
- Akses ke konten eksklusif
- Kualitas streaming yang lebih tinggi

### 🔧 **Fitur Teknis Lanjutan**
- **Index Management**: Mengelola berbagai sumber konten (GDIndex, GoIndex, MapleIndex, SimpleProgram)
- **Database Sync**: Sinkronisasi database otomatis dengan server
- **Offline Support**: Download konten untuk ditonton offline
- **Multi-platform**: Mendukung Android TV dan smartphone

## Arsitektur Aplikasi

### **Komponen Utama:**

1. **MainActivity** - Activity utama dengan navigasi bottom navigation
2. **PlayerFragment** - Fragment untuk memutar video dengan ExoPlayer
3. **HomeFragment** - Halaman utama dengan rekomendasi dan trending
4. **SearchFragment** - Fitur pencarian konten
5. **LibraryFragment** - Perpustakaan media pengguna
6. **SettingsFragment** - Pengaturan aplikasi

### **Teknologi yang Digunakan:**

- **Framework**: Android Native (Java)
- **Database**: Room Database untuk penyimpanan lokal
- **Authentication**: Firebase Auth
- **Media Player**: ExoPlayer/Media3
- **Image Loading**: Glide
- **Backend**: Firebase Realtime Database
- **Analytics**: Firebase Analytics
- **Ads**: Google AdMob

### **Sumber Konten:**
Aplikasi mendukung berbagai jenis index server:
- **GDIndex**: Google Drive sebagai sumber konten
- **GoIndex**: Index server berbasis Go
- **MapleIndex**: Index server Maple
- **SimpleProgram**: Program sederhana untuk indexing

## Cara Penggunaan

1. **Download dan Install** aplikasi NFGPlus
2. **Daftar/Login** menggunakan akun Google atau email
3. **Scan Library** untuk memuat konten dari server
4. **Browse** katalog film dan serial TV
5. **Pilih** konten yang ingin ditonton
6. **Stream** langsung atau download untuk offline
7. **Upgrade** ke premium untuk pengalaman tanpa iklan

## Target Pengguna

- Pecinta film dan serial TV
- Pengguna yang mencari alternatif streaming legal
- Penggemar konten Asia (khususnya drakor)
- Pengguna yang menginginkan fleksibilitas streaming

## Keunggulan Aplikasi

✅ **Gratis dengan opsi premium**
✅ **Interface yang user-friendly**
✅ **Katalog konten yang luas**
✅ **Kualitas streaming yang baik**
✅ **Dukungan download offline**
✅ **Regular update konten**
✅ **Kompatibel dengan Android TV**

---

**Versi Aplikasi**: Asthadasa (v80)
**Package Name**: com.gelaskaca.nfgplus
**Target SDK**: 36 (Android 14+)