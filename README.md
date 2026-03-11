# NFGPlus - Modern Android Streaming Application

**NFGPlus** has been modernized to a robust, **Offline-First** streaming application powered by **Cloudflare D1** and **Kotlin**. It allows users to stream movies and TV shows from Google Drive sources with a seamless synchronization experience.

### 📺 **Advanced Media Playback**
- **LibVLC Engine**: Powered by **LibVLC 3.6.4** for broad codec support and hardware acceleration.
- **Custom Player UI**: Interactive controls with swipe gestures for volume, brightness, and seeking.
- **Dynamic Selection**: SideSheets/BottomSheets for real-time subtitle and audio track switching.
- **Smart Features**: Integrated **Sleep Timer**, **Picture-in-Picture (PiP)**, and **Screen Lock**.

### 🔄 **Hybrid Synchronization (Smart Sync)**
- **Offline-First**: Uses pre-populated Room Database (`nfgplus.db`) for instant load times.
- **Centralized History**: Uses a local JSON-based history cache in `SyncPrefs` for instant "Continue Watching" updates.
- **Google TV Integration**: Syncs movies and TV shows to the home screen via **Google Engage SDK** (Recommendations/Watch Next).
- **Delta Sync**: Efficiently fetches only modified data using incremental timestamps.

---

## 🏗️ Architecture

### **Android Client**
- **Language**: Kotlin 100%
- **DI**: Hilt (Dagger)
- **Concurrency**: Coroutines & Flow
- **Local DB**: Room Database (SQLite)
- **Video Engine**: **LibVLC 3.6.4**
- **UI**: XML ViewSystem + **Jetpack Compose** (Modular integration)
- **Background**: WorkManager

### **Backend (Serverless)**
- **Platform**: Cloudflare Workers
- **Database**: Cloudflare D1 (Serverless SQLite)
- **Indexing**: Automated Python scripts via GitHub Actions.

---

## 🛠️ Setup & Installation

### 1. Android Client
1.  Open the `app` folder in Android Studio.
2.  Create `local.properties` (if missing) and add keys.
3.  Build and Run:
    ```bash
    ./gradlew assembleDebug
    ```

### 2. Cloudflare Backend
1.  Navigate to `cloudflare-backend`:
    ```bash
    cd cloudflare-backend
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Deploy to Cloudflare:
    ```bash
    wrangler deploy
    ```
    *Make sure to configure `wrangler.toml` with your specific D1 Database IDs.*

### 3. GitHub Actions (Automation)
1.  Go to your Repository **Settings** -> **Secrets and variables** -> **Actions**.
2.  Add the following secrets:
    -   `ADMIN_KEY`: Your Worker's Admin API Key (from `wrangler.toml`).
    -   `TMDB_KEY`: Your TMDB API Key.
    -   `(Optional)` `GD_USER` & `GD_PASS`: If your GDIndex is password protected.
3.  The workflow `.github/workflows/scan_gdindex.yml` will run automatically.

---

## 📦 Data Migration Notes

- **Asset Database**: The app ships with a `nfgplus.db` asset. This version (`v32`) uses **snake_case** column names.
- **Migration**: The app automatically handles migration from legacy versions, but a clean install is recommended for the best performance with the new sync engine.

---

## 📝 License
Proprietary Software. All rights reserved.