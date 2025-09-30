# ZulMIA - Inventory Count Application (Android/Java)

This is an Android app (Java, SQLite) for inventory counting using a handheld barcode scanner (keyboard wedge input). It supports login/registration, creating/resuming inventories, scanning by location/shelf, compact cards with live quantity adjustment, CSV export, and an optional offline license gate to restrict usage to authorized devices.

## Features
- Local auth: username/password stored in SQLite
- Inventory sessions: create, resume, submit, submit & finish
- Scanning: location → shelf → item (no camera; physical scanner inputs text with Enter)
- Cards: barcode + quantity (+/− buttons), location & shelf labels; swipe to remove row
- Export: CSV to Downloads/ZulMIA with clickable Open action
- Splash: logo + title + “Developed by” (loads `Downloads/ZulMIA/Zultec.png` if present)
- License gate (optional, enabled): app runs only if a valid signed license file is present

## Tech
- Language: Java
- Min/Target SDK: 24/34
- UI: AppCompat, Material, RecyclerView
- Storage: SQLite (SQLiteOpenHelper)

## Project structure (main parts)
- `app/src/main/java/com/zulmia/app/ui` – Activities & adapters
  - `SplashActivity` – shows splash, verifies license, navigates to Login
  - `LoginActivity` – login/registration
  - `InventoryActivity` – create/resume inventories (pending list)
  - `ScanActivity` – scanning screen with cards, submit controls
  - `CsvViewerActivity` – in-app CSV viewer fallback
  - `ScanAdapter` / `InventoryAdapter` – RecyclerView adapters
  - `BaseActivity` – toolbar + logout menu
- `app/src/main/java/com/zulmia/app/data`
  - `DatabaseHelper` – SQLite schema/migrations
  - `InventoryRepository` – DB operations (users, inventories, scans)
- `app/src/main/java/com/zulmia/app/util`
  - `CsvExporter` – exports CSV to Downloads/ZulMIA via MediaStore (scoped storage)
  - `SessionManager` – session SharedPreferences
  - `LicenseVerifier` – offline license verification
- `app/src/main/res/layout` – layouts: `activity_*`, `item_*`, `include_toolbar`
- `app/src/main/AndroidManifest.xml` – launcher: `SplashActivity`

## Build & Run
### Android Studio
1. Open the project folder in Android Studio.
2. Build → Make Project.
3. Run on a connected device (the scanner must act as keyboard input).

### CLI (macOS)
Prereqs: Android SDK, `adb` on PATH.
```bash
# Build debug
./gradlew :app:assembleDebug

# Install & launch
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.zulmia.app/.ui.SplashActivity
```
Tip: Mirror with scrcpy (optional): `scrcpy`.

## App Flow
1. Splash: shows branding and checks license.
2. Login/Registration (local SQLite).
3. New Inventory: enter a unique name (e.g., 15July-Branch1) → Start Scan.
4. Scan screen:
   - Focus order: Location → Shelf → Item. Soft keyboard is suppressed until you tap.
   - Scanner sends barcode + Enter; identical scans aggregate quantity.
   - Cards: plus/minus to adjust count (does not reorder); swipe to remove row.
   - Submit → save only; Submit & Finish → export CSV and mark completed.

## Scanning behavior
- Any scanned string is stored directly (no parsing).
- Aggregation key: code + location + shelf.
- New scans appear latest-first. Manual +/− changes do NOT move items to top.

## CSV Export
- Path: Downloads/ZulMIA
- Filename: `<inventoryName>-<YYYYMMDD-HHMMSS>.csv`
- Columns: Barcode/QR Code, Quantity, Location, Shelf, User, Timestamp
- Snackbar offers Open; uses CSV viewer or in-app viewer fallback.

## Splash Branding
- Title: “ZulMIA - Inventory Count Application”
- “Developed by” below the title, then brand image if present.
- Optional brand file: `/sdcard/Download/ZulMIA/Zultec.png`

## Offline License Gate (quick option)
The app runs only if `license.bin` exists and verifies with the embedded RSA public key.

Supported license locations (either):
- App-scoped: `/sdcard/Android/data/com.zulmia.app/files/license.bin`
- Public: `/sdcard/Download/ZulMIA/license.bin`

### 1) Create your key pair (on your Mac)
```bash
# Private key
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out zulmia_private.pem

# Public key (Base64 DER, single line)
openssl rsa -in zulmia_private.pem -pubout -outform DER | base64 | tr -d '\n'
```

### 2) Configure the app to trust your public key
- Open `app/src/main/java/com/zulmia/app/util/LicenseVerifier.java`
- Replace `PUBLIC_KEY_BASE64` with the Base64 from the previous step.
- Rebuild and reinstall the app.

### 3) Find deviceId on a target device
```bash
adb shell settings get secure android_id
adb shell getprop ro.product.model
```
Compose: `<android_id>-<model>` (e.g., `1234abcd5678efgh-Urovo DT50`).

### 4) Generate license.bin for that device
```bash
# Variables
DEVICE_ID="<android_id>-<model>"
EXPIRY="2026-12-31T23:59:59Z"   # UTC ISO 8601

# Payload (no newline)
PAYLOAD="${DEVICE_ID}|${EXPIRY}"

# Signature (Base64)
echo -n "$PAYLOAD" | openssl dgst -sha256 -sign zulmia_private.pem | base64 | tr -d '\n' > sig.txt
SIG=$(cat sig.txt)

# license.bin (single line JSON)
cat > license.bin <<EOF
{"deviceId":"${DEVICE_ID}","expiry":"${EXPIRY}","sig":"${SIG}"}
EOF
```

### 5) Install the license onto the device
```bash
adb shell mkdir -p /sdcard/Android/data/com.zulmia.app/files
adb push license.bin /sdcard/Android/data/com.zulmia.app/files/license.bin
# or
adb shell mkdir -p /sdcard/Download/ZulMIA
adb push license.bin /sdcard/Download/ZulMIA/license.bin
```

### 6) Test
```bash
adb shell am force-stop com.zulmia.app
adb shell am start -n com.zulmia.app/.ui.SplashActivity
```
If valid: app proceeds. If missing/invalid/expired: app shows a message and exits.

### Renew / Disable
- Renew: generate a new `license.bin` with a later expiry and push it to the device.
- Disable: remove `license.bin` from the device.

### Security Notes
- Keep `zulmia_private.pem` safe; only the public key is embedded in the app.
- Offline licensing is practical but not tamper-proof. For stronger control, combine with Android Enterprise (MDM) and/or server-side attestation (Play Integrity / Key Attestation).

## Troubleshooting
- License error at splash: confirm `license.bin` path, deviceId, expiry format `YYYY-MM-DDThh:mm:ssZ` (UTC), and that the public key in `LicenseVerifier` matches your private key.
- CSV won’t open: install a CSV viewer or rely on the in-app viewer via Snackbar → Open.
- Zultec branding not shown: ensure `Downloads/ZulMIA/Zultec.png` exists.
- Items jump to top on +/-: fixed — manual quantity change does not update timestamp.
