#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Script Build Modul DualLineStatusBar di Termux
# ==============================================================================

set -e

PROJECT_SRC="/storage/emulated/0/Documents/DualLineStatusBar"
TARGET_DIR="$HOME/DualLineStatusBar"

echo "=========================================="
echo "🚀 DualLineStatusBar - Termux Build Script"
echo "=========================================="

# 1. Pastikan dependencies di Termux terinstall
echo "📦 Memeriksa dependencies (openjdk-17, gradle)..."
if ! command -v java >/dev/null 2>&1; then
    echo "⚠️ Java belum terpasang. Menginstall openjdk-17..."
    pkg update -y && pkg install -y openjdk-17
fi

if ! command -v gradle >/dev/null 2>&1; then
    echo "⚠️ Gradle belum terpasang. Menginstall gradle..."
    pkg install -y gradle
fi

# 2. Sinkronisasi project ke Termux internal storage (agar exec permission valid)
echo "📂 Menyinkronkan file project ke $TARGET_DIR..."
mkdir -p "$TARGET_DIR"
cp -r "$PROJECT_SRC"/* "$TARGET_DIR"/
cd "$TARGET_DIR"

# 3. Generate Gradle Wrapper jika belum ada
if [ ! -f "gradlew" ]; then
    echo "⚙️ Menyiapkan Gradle Wrapper (v8.2.2)..."
    gradle wrapper --gradle-version 8.2.2 --distribution-type bin
    chmod +x gradlew
fi

# 4. Build APK
echo "🔨 Memulai proses compile (assembleDebug)..."
./gradlew assembleDebug --stacktrace

APK_PATH="$TARGET_DIR/app/build/outputs/apk/debug/app-debug.apk"
SHARED_APK="/storage/emulated/0/Documents/DualLineStatusBar-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "✅ BUILD BERHASIL!"
    cp "$APK_PATH" "$SHARED_APK"
    echo "📦 APK disalin ke: $SHARED_APK"
    echo ""
    echo "Langkah selanjutnya:"
    echo "1. Buka File Manager atau APK Installer di HP."
    echo "2. Install DualLineStatusBar-debug.apk dari folder Documents."
    echo "3. Buka aplikasi LSPosed Manager -> Tab Modules."
    echo "4. Aktifkan 'DualLine StatusBar' dan centang scope 'System UI' (com.android.systemui)."
    echo "5. Buka aplikasi DualLine StatusBar sekali (agar preference terinisialisasi)."
    echo "6. Reboot HP atau restart SystemUI (pkill -f com.android.systemui)."
else
    echo "❌ APK tidak ditemukan setelah build."
    exit 1
fi
