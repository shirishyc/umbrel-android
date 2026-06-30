# UmbrelOS Android App

A native Android app for managing your [UmbrelOS](https://umbrel.com) home server.

## Features

- **Dashboard** — System overview with CPU, memory, disk gauges
- **App Store** — Browse, search, and install apps from the Umbrel App Store
- **My Apps** — Start, stop, restart, and uninstall installed apps
- **Files** — Browse, preview, and download files from your Umbrel
- **System Status** — Live hardware monitoring with reboot/shutdown controls
- **WiFi** — Scan networks and manage connections
- **Backups** — Create and restore system backups
- **Notifications** — View and dismiss system notifications
- **Settings** — User preferences, theme, language, Tor toggle
- **Real-time updates** — Live dashboard via WebSocket
- **Auto-discovery** — Finds your Umbrel on the local network via mDNS
- **QR setup** — Scan your Umbrel's setup QR code to auto-configure
- **Offline cache** — Snappy startup even without network

## Requirements

- **Android 8.0+** (API 26)
- **Google Play Services** (for ML Kit barcode scanning)
- Device on the **same network** as your UmbrelOS server

## Build Instructions

### Option 1: GitHub Actions (online, no setup needed) ⭐

Push the project to a **GitHub repository** and the APK builds automatically:

1. **Create a GitHub repo** at [github.com/new](https://github.com/new)
2. **Upload the project:**
   ```bash
   cd C:\Users\PIHU\umbrel-android
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USER/umbrel-android.git
   git push -u origin main
   ```
3. Go to your repo → **Actions** tab → "Build APK" workflow runs automatically
4. When done, click the workflow run → scroll to **Artifacts** section
5. Download `umbrel-android-debug.zip` — inside is your **APK**!
6. Sideload the APK onto your phone (Settings → Security → Install unknown apps)

### Option 2: Android Studio (local)

1. **Install Android Studio** from [developer.android.com/studio](https://developer.android.com/studio)
2. **Clone or extract** this project
3. Open Android Studio → **File → Open** → select the project folder
4. Wait for **Gradle sync** to complete (downloads dependencies automatically)
5. Connect your Android device via USB (enable Developer Options + USB Debugging)
6. Click **Run** (▶) or `Shift+F10`

### Option 2: Command line

```bash
# Set Android SDK path (adjust for your system)
export ANDROID_HOME=$HOME/Android/Sdk   # Linux/Mac
set ANDROID_HOME=C:\Users\<you>\AppData\Local\Android\Sdk   # Windows

# Build debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: Release APK (signed)

1. Open project in Android Studio
2. **Build → Generate Signed Bundle / APK**
3. Create a keystore or use an existing one
4. Select **release** build variant
5. Signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

## First Use

1. Launch the app
2. The app will scan for UmbrelOS servers on your network
3. Tap your discovered server, or tap **Scan QR Code** to scan the QR from your Umbrel dashboard
4. Enter your admin password and tap **Sign In**
5. You're in!

## Tech Stack

| Layer | Technology |
|---|---|
| Language | **Kotlin** |
| UI | **Jetpack Compose** + Material 3 |
| Architecture | **MVVM** (ViewModels + StateFlow) |
| Networking | **OkHttp** (tRPC + WebSocket + REST) |
| Serialization | **Kotlinx Serialization** |
| DI | **Hilt** |
| Cache | **Room** (SQLite) |
| Discovery | **JmDNS** (mDNS/Bonjour) |
| QR Scanner | **CameraX** + **ML Kit** |
| Image Loading | **Coil** |
| Auth | **EncryptedSharedPreferences** (AES256-GCM) |

## Project Structure

```
umbrel-android/
├── app/
│   └── src/main/java/com/umbrel/android/
│       ├── navigation/          # Screen routes + NavGraph
│       ├── ui/screens/          # 13 screen packages
│       │   ├── setup/           # Server URL entry + mDNS discovery
│       │   ├── login/           # Password login
│       │   ├── dashboard/       # System overview
│       │   ├── appstore/        # App browser + detail
│       │   ├── apps/            # My Apps management
│       │   ├── system/          # Hardware monitoring
│       │   ├── files/           # File browser + preview
│       │   ├── backups/         # Backup management
│       │   ├── wifi/            # Network scanner
│       │   ├── settings/        # Preferences + logout
│       │   ├── notifications/   # Notification list
│       │   └── qrscanner/       # QR code camera scanner
│       ├── data/
│       │   ├── models/          # Serializable data models
│       │   ├── api/             # 9 tRPC API wrappers
│       │   └── local/           # Room database + cache
│       ├── core/
│       │   ├── network/         # tRPC client, WebSocket, file transfer
│       │   ├── auth/            # Login, JWT store
│       │   └── discovery/       # mDNS server discovery
│       ├── ui/theme/            # Dark Material 3 theme
│       ├── UmbrelApp.kt        # Application class
│       └── MainActivity.kt     # Compose entry point
├── build.gradle.kts             # Root build
├── settings.gradle.kts          # Project settings
└── gradle/libs.versions.toml   # Version catalog
```

## API Architecture

The app communicates with UmbrelOS via **tRPC** (JSON-RPC over HTTP POST):

```
POST /trpc
{"method":"query","params":["system.status"]}

Response:
{"result":{"data":{"version":"1.7.3","updateAvailable":false,...}}}
```

**Auth:** `user.login` mutation → JWT → `Authorization: Bearer <token>` header
**Real-time:** WebSocket at `ws://<host>/trpc?token=<jwt>`
**Files:** REST at `/api/files/*` (cookie auth via UMBREL_PROXY_TOKEN)

## License

MIT
