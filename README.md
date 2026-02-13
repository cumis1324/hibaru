# NFGPlus - Modern Android Streaming Application

**NFGPlus** has been modernized to a robust, **Offline-First** streaming application powered by **Cloudflare D1** and **Kotlin**. It allows users to stream movies and TV shows from Google Drive sources with a seamless synchronization experience.

## 🚀 Key Features

### 🔄 **Hybrid Synchronization (Smart Sync)**
- **Offline-First**: Uses pre-populated Room Database (`nfgplus.db`) for instant load times.
- **Delta Sync**: Only fetches data modified since the last sync timestamp, saving bandwidth.
- **Background Sync**: `SyncWorker` keeps data fresh automatically every 24 hours.

### 🎭 **Dynamic Demo Mode**
- **Role-Based Data**: Automatically switches database sources based on the logged-in user.
- **Admin/Demo User**: Connects to a dedicated `nfgplus-demo-db` for testing without affecting production data.
- **Auto-Wipe**: Ensures clean state transition when switching between Production and Demo modes.

### 🤖 **Automated Content Indexing**
- **GitHub Actions**: Scheduled workflows automatically scan GDIndex sources (Google Drive) 4 times a day.
- **Sequential Scanning**: Prioritizes Movies, then TV Series to ensuring orderly updates.
- **Manual Trigger**: Admins can trigger specific scans manually via GitHub UI.

### 📺 **Modern Android Experience**
- **Native Player**: Custom UI on top of Media3/ExoPlayer for high-performance playback.
- **TV Show Support**: Complete support for Seasons and Episodes with intricate metadata.
- **Search & Filter**: Fast local search and filtering by genre, year, and popularity.

---

## 🏗️ Architecture

### **Android Client**
- **Language**: Kotlin 100%
- **DI**: Hilt (Dagger)
- **Concurrency**: Coroutines & Flow
- **Local DB**: Room Database (SQLite)
- **Network**: Retrofit + OkHttp
- **Background**: WorkManager

### **Backend (Serverless)**
- **Platform**: Cloudflare Workers
- **Language**: TypeScript
- **Database**: Cloudflare D1 (Serverless SQLite)
- **API**: RESTful endpoints with bearer token authentication.

### **Automation**
- **Script**: Python (`import_gdindex.py`) using `requests` and `thefuzz` for TMDB matching.
- **Scheduler**: GitHub Actions Cron Job.

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