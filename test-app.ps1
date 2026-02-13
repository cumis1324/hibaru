# NFGPlus Android Testing Script
# Usage: .\test-app.ps1 [command]

param(
    [Parameter(Position=0)]
    [string]$Command = "help"
)

$APP_PACKAGE = "com.theflexproject.thunder"
$MAIN_ACTIVITY = "$APP_PACKAGE/.SplashScreenActivity"

function Write-Header {
    param([string]$Text)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host " $Text" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
}

function Check-Device {
    Write-Host "Checking connected devices..." -ForegroundColor Yellow
    $devices = adb devices | Select-String "device$"
    
    if ($devices.Count -eq 0) {
        Write-Host "❌ No devices connected!" -ForegroundColor Red
        Write-Host "Please connect your phone via USB and enable USB Debugging." -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "✅ Device connected" -ForegroundColor Green
}

function Build-App {
    Write-Header "Building Debug APK"
    .\gradlew assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Build successful!" -ForegroundColor Green
    } else {
        Write-Host "❌ Build failed!" -ForegroundColor Red
        exit 1
    }
}

function Install-App {
    Write-Header "Installing App to Device"
    .\gradlew installDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ App installed successfully!" -ForegroundColor Green
    } else {
        Write-Host "❌ Installation failed!" -ForegroundColor Red
        exit 1
    }
}

function Launch-App {
    Write-Header "Launching App"
    adb shell am start -n $MAIN_ACTIVITY
    Write-Host "✅ App launched" -ForegroundColor Green
}

function Stop-App {
    Write-Header "Stopping App"
    adb shell am force-stop $APP_PACKAGE
    Write-Host "✅ App stopped" -ForegroundColor Green
}

function Clear-AppData {
    Write-Header "Clearing App Data"
    adb shell pm clear $APP_PACKAGE
    Write-Host "✅ App data cleared (will trigger fresh sync)" -ForegroundColor Green
}

function Uninstall-App {
    Write-Header "Uninstalling App"
    adb uninstall $APP_PACKAGE
    Write-Host "✅ App uninstalled" -ForegroundColor Green
}

function Watch-Logs {
    Write-Header "Watching Sync Logs (Ctrl+C to stop)"
    adb logcat -c  # Clear logs first
    adb logcat | Select-String "SyncManager|SyncActivity|SyncWorker|NFGPlusApi"
}

function Watch-AllLogs {
    Write-Header "Watching All App Logs (Ctrl+C to stop)"
    adb logcat -c
    adb logcat | Select-String "thunder"
}

function Watch-Errors {
    Write-Header "Watching Error Logs (Ctrl+C to stop)"
    adb logcat -c
    adb logcat *:E
}

function Pull-Database {
    Write-Header "Pulling Database from Device"
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $outputFile = "MyToDos_$timestamp.db"
    
    adb exec-out run-as $APP_PACKAGE cat databases/MyToDos > $outputFile
    
    if (Test-Path $outputFile) {
        $size = (Get-Item $outputFile).Length / 1MB
        Write-Host "✅ Database saved: $outputFile ($([math]::Round($size, 2)) MB)" -ForegroundColor Green
        
        # Quick query to show counts
        Write-Host "`nDatabase contents:" -ForegroundColor Yellow
        $movieCount = sqlite3 $outputFile "SELECT COUNT(*) FROM Movie"
        $showCount = sqlite3 $outputFile "SELECT COUNT(*) FROM TVShow"
        $episodeCount = sqlite3 $outputFile "SELECT COUNT(*) FROM Episode"
        
        Write-Host "  Movies: $movieCount" -ForegroundColor White
        Write-Host "  TV Shows: $showCount" -ForegroundColor White
        Write-Host "  Episodes: $episodeCount" -ForegroundColor White
    } else {
        Write-Host "❌ Failed to pull database" -ForegroundColor Red
    }
}

function Test-FreshInstall {
    Write-Header "Test: Fresh Install + First Sync"
    
    Check-Device
    
    Write-Host "Step 1: Uninstall existing app..." -ForegroundColor Yellow
    adb uninstall $APP_PACKAGE 2>$null
    
    Write-Host "Step 2: Build app..." -ForegroundColor Yellow
    Build-App
    
    Write-Host "Step 3: Install app..." -ForegroundColor Yellow
    Install-App
    
    Write-Host "Step 4: Clear logcat..." -ForegroundColor Yellow
    adb logcat -c
    
    Write-Host "Step 5: Launch app and watch sync..." -ForegroundColor Yellow
    Launch-App
    
    Start-Sleep -Seconds 2
    
    Write-Host "`n📱 App launched! Watching sync logs..." -ForegroundColor Green
    Write-Host "   (Ctrl+C to stop watching)" -ForegroundColor Yellow
    Watch-Logs
}

function Test-QuickRestart {
    Write-Header "Test: Quick Restart (No Sync Expected)"
    
    Check-Device
    
    Write-Host "Step 1: Stop app..." -ForegroundColor Yellow
    Stop-App
    
    Write-Host "Step 2: Clear logcat..." -ForegroundColor Yellow
    adb logcat -c
    
    Write-Host "Step 3: Launch app..." -ForegroundColor Yellow
    Launch-App
    
    Start-Sleep -Seconds 2
    
    Write-Host "`n📱 App launched! Should skip sync (< 24h)..." -ForegroundColor Green
    Write-Host "   Expected: 'Loading database...' → 'Enjoy!'" -ForegroundColor Yellow
    Watch-Logs
}

function Test-ForceSync {
    Write-Header "Test: Force Sync (Clear Preferences)"
    
    Check-Device
    
    Write-Host "Step 1: Clear app data (including sync prefs)..." -ForegroundColor Yellow
    Clear-AppData
    
    Write-Host "Step 2: Clear logcat..." -ForegroundColor Yellow
    adb logcat -c
    
    Write-Host "Step 3: Launch app and watch sync..." -ForegroundColor Yellow
    Launch-App
    
    Start-Sleep -Seconds 2
    
    Write-Host "`n📱 App launched! Should trigger full sync..." -ForegroundColor Green
    Watch-Logs
}

function Show-Help {
    Write-Host ""
    Write-Host "NFGPlus Android Testing Script" -ForegroundColor Cyan
    Write-Host "==============================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "BASIC COMMANDS:" -ForegroundColor Yellow
    Write-Host "  .\test-app.ps1 check          - Check if device is connected"
    Write-Host "  .\test-app.ps1 build          - Build debug APK"
    Write-Host "  .\test-app.ps1 install        - Install app to device"
    Write-Host "  .\test-app.ps1 launch         - Launch app"
    Write-Host "  .\test-app.ps1 stop           - Stop app"
    Write-Host "  .\test-app.ps1 restart        - Stop and restart app"
    Write-Host "  .\test-app.ps1 uninstall      - Uninstall app"
    Write-Host ""
    Write-Host "DATABASE COMMANDS:" -ForegroundColor Yellow
    Write-Host "  .\test-app.ps1 clear          - Clear app data (triggers fresh sync)"
    Write-Host "  .\test-app.ps1 pull-db        - Pull database from device"
    Write-Host ""
    Write-Host "LOG COMMANDS:" -ForegroundColor Yellow
    Write-Host "  .\test-app.ps1 logs           - Watch sync-related logs"
    Write-Host "  .\test-app.ps1 logs-all       - Watch all app logs"
    Write-Host "  .\test-app.ps1 logs-error     - Watch error logs only"
    Write-Host ""
    Write-Host "TEST WORKFLOWS:" -ForegroundColor Yellow
    Write-Host "  .\test-app.ps1 test-fresh     - Test fresh install + first sync"
    Write-Host "  .\test-app.ps1 test-restart   - Test restart (no sync expected)"
    Write-Host "  .\test-app.ps1 test-sync      - Test force sync (clear prefs)"
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Yellow
    Write-Host "  # Quick test after code changes"
    Write-Host "  .\test-app.ps1 build"
    Write-Host "  .\test-app.ps1 install"
    Write-Host "  .\test-app.ps1 restart"
    Write-Host ""
    Write-Host "  # Test full sync flow"
    Write-Host "  .\test-app.ps1 test-fresh"
    Write-Host ""
    Write-Host "  # Debug sync issues"
    Write-Host "  .\test-app.ps1 clear"
    Write-Host "  .\test-app.ps1 launch"
    Write-Host "  .\test-app.ps1 logs"
    Write-Host ""
}

# Main script logic
switch ($Command.ToLower()) {
    "check" { Check-Device }
    "build" { Build-App }
    "install" { 
        Check-Device
        Install-App 
    }
    "launch" { 
        Check-Device
        Launch-App 
    }
    "stop" { 
        Check-Device
        Stop-App 
    }
    "restart" {
        Check-Device
        Stop-App
        Start-Sleep -Seconds 1
        Launch-App
    }
    "clear" { 
        Check-Device
        Clear-AppData 
    }
    "uninstall" { 
        Check-Device
        Uninstall-App 
    }
    "logs" { 
        Check-Device
        Watch-Logs 
    }
    "logs-all" { 
        Check-Device
        Watch-AllLogs 
    }
    "logs-error" { 
        Check-Device
        Watch-Errors 
    }
    "pull-db" { 
        Check-Device
        Pull-Database 
    }
    "test-fresh" { Test-FreshInstall }
    "test-restart" { Test-QuickRestart }
    "test-sync" { Test-ForceSync }
    "help" { Show-Help }
    default { 
        Write-Host "Unknown command: $Command" -ForegroundColor Red
        Show-Help 
    }
}
