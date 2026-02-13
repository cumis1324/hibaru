# NFGPlus Testing Guide

## Prerequisites

1. **Android Phone Setup**
   - Enable **Developer Options** (tap Build Number 7x)
   - Enable **USB Debugging** in Developer Options
   - Connect phone via USB cable

2. **Required Tools**
   - ADB (Android Debug Bridge) - included with Android SDK
   - SQLite3 (optional, for database inspection)

3. **Verify Setup**
   ```powershell
   adb devices
   # Should show your device
   ```

---

## Quick Start

### 1. First Time Setup
```powershell
# Connect phone and run
.\test-app.ps1 test-fresh
```

This will:
- Uninstall any existing app
- Build fresh debug APK
- Install to device
- Launch app
- Watch sync logs in real-time

### 2. After Code Changes
```powershell
# Quick rebuild and test
.\test-app.ps1 build
.\test-app.ps1 install
.\test-app.ps1 restart
.\test-app.ps1 logs
```

### 3. Test Sync Logic
```powershell
# Force a fresh sync
.\test-app.ps1 test-sync
```

---

## Common Testing Scenarios

### Scenario 1: Test Fresh Install (Full Sync)

**What to test:** First-time user experience, full data sync

```powershell
.\test-app.ps1 test-fresh
```

**Expected behavior:**
- SyncActivity shows "Checking for updates..."
- Progress: "Syncing Movies... 33%"
- Progress: "Syncing TV Shows... 66%"
- Progress: "Syncing Episodes... 100%"
- "Sync complete!"
- Navigate to MainActivity

**Expected logs:**
```
SyncActivity: Starting background sync
SyncManager: Starting full sync...
SyncManager: Movies batch synced: 7, total: 7
SyncManager: TV Shows batch synced: 4, total: 4
SyncManager: Episodes for show The Office: 8
SyncManager: Full sync completed: 19 items updated
SyncActivity: Sync complete!
```

---

### Scenario 2: Test Quick Restart (Skip Sync)

**What to test:** User reopens app within 24 hours

```powershell
.\test-app.ps1 test-restart
```

**Expected behavior:**
- SyncActivity shows "Loading database..."
- Quick check: last sync < 24h
- Skip sync, show "Enjoy!"
- Navigate to MainActivity immediately

**Expected logs:**
```
SyncActivity: Checking sync status
SyncManager: Last sync: 5 minutes ago
SyncActivity: Sync not needed, loading database
SyncActivity: Navigate to MainActivity
```

---

### Scenario 3: Test Background Sync

**What to test:** WorkManager periodic sync (24-hour interval)

```powershell
# Manually trigger background sync worker
adb shell am broadcast -a androidx.work.testing.SYNC_WORKER

# Watch logs
.\test-app.ps1 logs
```

**Expected logs:**
```
SyncWorker: Starting background sync (attempt 1)
SyncManager: Checking if sync needed
SyncManager: Last sync: 25 hours ago
SyncManager: Starting full sync...
SyncWorker: Background sync completed successfully: 19 items updated
```

---

### Scenario 4: Test Offline Behavior

**What to test:** App behavior without network

```powershell
# 1. Turn off WiFi/Data on phone
# 2. Clear app data to force sync
.\test-app.ps1 clear

# 3. Launch and watch
.\test-app.ps1 launch
.\test-app.ps1 logs
```

**Expected behavior:**
- SyncActivity shows sync progress
- API calls fail (network error)
- SyncManager shows "Sync failed: Unable to resolve host..."
- Retry dialog appears
- User can "Skip" to use cached data

---

### Scenario 5: Inspect Database

**What to test:** Verify data synced correctly

```powershell
# Pull database from device
.\test-app.ps1 pull-db

# Output shows:
# ✅ Database saved: MyToDos_20260210_151234.db (2.5 MB)
# 
# Database contents:
#   Movies: 7
#   TV Shows: 4
#   Episodes: 8
```

Manual inspection:
```powershell
# Open with SQLite browser or query directly
sqlite3 MyToDos_20260210_151234.db

# Check movie titles
SELECT title, release_date FROM Movie LIMIT 5;

# Check TV show details
SELECT name, number_of_seasons FROM TVShow;

# Check episodes
SELECT name, season_number, episode_number FROM Episode WHERE show_id = 1;
```

---

## Testing Checklist

### ✅ Before Committing Code

- [ ] `.\test-app.ps1 test-fresh` - Fresh install works
- [ ] `.\test-app.ps1 test-restart` - Skip sync works
- [ ] `.\test-app.ps1 test-sync` - Force sync works
- [ ] Test offline: Retry dialog appears
- [ ] Test with demo DB (7 movies, 4 shows, 8 episodes)
- [ ] Pull database: Verify counts match
- [ ] No crash logs in `.\test-app.ps1 logs-error`

### ✅ Before Release

- [ ] Test on multiple devices (different Android versions)
- [ ] Test with production DB (6,262 movies, 553 shows)
- [ ] Measure sync time (should be < 60 seconds for demo)
- [ ] Test background sync after 24 hours
- [ ] Test with slow network (throttle in Developer Options)
- [ ] Test with interrupted sync (airplane mode mid-sync)
- [ ] Memory usage during sync (< 200 MB)

---

## Troubleshooting

### Device Not Detected

```powershell
# Check USB cable connection
adb devices

# If shows "unauthorized", check phone for permission dialog
# If no devices, try:
adb kill-server
adb start-server
adb devices
```

### App Won't Install

```powershell
# Uninstall completely first
.\test-app.ps1 uninstall

# Clean build
.\gradlew clean
.\test-app.ps1 build
.\test-app.ps1 install
```

### Sync Fails Immediately

```powershell
# Check API connectivity
curl https://nfgplus-backend.worker1-b8f.workers.dev/api/health

# Check device network
adb shell ping -c 3 8.8.8.8

# View error details
.\test-app.ps1 logs-error
```

### Database Not Pulling

```powershell
# Manual method
adb shell "run-as com.theflexproject.thunder cat databases/MyToDos" > MyToDos.db

# Or use Android Studio Device File Explorer
# View > Tool Windows > Device File Explorer
# Navigate to /data/data/com.theflexproject.thunder/databases/
```

---

## Performance Benchmarks

**Target Performance (Demo DB):**
- First sync: < 30 seconds
- App startup (no sync): < 2 seconds
- Database size: ~2-5 MB
- Memory usage: < 200 MB during sync

**Target Performance (Production DB):**
- First sync: < 3 minutes
- App startup (no sync): < 2 seconds
- Database size: ~50-100 MB
- Memory usage: < 300 MB during sync

---

## Continuous Testing

**During Development:**
```powershell
# Quick iteration loop
while ($true) {
    .\test-app.ps1 build
    .\test-app.ps1 install
    .\test-app.ps1 restart
    
    Write-Host "Press Enter to rebuild, Ctrl+C to exit..."
    Read-Host
}
```

**Automated Nightly Test:**
Create scheduled task to run:
```powershell
.\test-app.ps1 test-fresh
.\test-app.ps1 pull-db
# Send results to Slack/Email
```

---

## Next Steps

After successful testing:
1. ✅ Update `walkthrough.md` with test results
2. ✅ Create unit tests for `SyncManager`
3. ✅ Setup CI/CD pipeline (GitHub Actions)
4. ✅ Beta testing with 10 users
5. ✅ Production rollout (gradual 10% → 50% → 100%)
