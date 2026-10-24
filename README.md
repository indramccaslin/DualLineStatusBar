# DualLineStatusBar 🚀
> **Modul LSPosed / Xposed untuk Android 15 & 17 (AOSP / Project Infinity X)**

DualLineStatusBar mengubah status bar Android standar (1 baris) menjadi **2 baris (Dual Line)** yang simetris, rapi, dan dapat dikustomisasi secara penuh melalui antarmuka aplikasi bawaan.

---

## 📱 Tampilan Tata Letak Status Bar

```text
[ Line 1 ] : [ 20:49:18 ]  [ CPU 50°  BAT 50° ]                [ 📶 WiFi ]
[ Line 2 ] : [ Sab, 24 Okt ]  [ 🔔 💬 ... ]        [ ↕ 0.00 KB/s ] [ 🔋 ]
```

### Pembagian Elemen:
- **Line 1 (Atas):**
  - **Kiri:** Jam (`HH:mm:ss`) berdampingan dengan Suhu CPU & Baterai (`CPU 50°  BAT 50°`).
  - **Kanan:** Indikator Sinyal Seluler & Wi-Fi (`StatusIconContainer`).
- **Line 2 (Bawah):**
  - **Kiri:** Tanggal (`Sab, 24 Okt`) berdampingan dengan deretan Ikon Notifikasi.
  - **Kanan:** Kecepatan Jaringan bawaan ROM (*Network Traffic*) berdampingan dengan Ikon Baterai.

---

## ✨ Fitur Utama

1. **Dual Line Layout Sempurna:**
   - Membagi tinggi status bar menjadi dua baris dengan rasio 50:50 yang presisi (`weight = 1.0f`).
   - Penjajaran vertikal dan padding otomatis disesuaikan agar tidak ada teks atau ikon yang terpotong.
2. **Stabilitas Posisi Jam & Tanggal:**
   - Jam dan tanggal ditempatkan dalam kolom terisolasi sehingga tidak bergeser saat notifikasi masuk atau dihapus.
3. **Info Suhu Hardware Real-Time:**
   - Menampilkan suhu CPU (dari thermal zone) dan suhu baterai (dari BatteryManager) yang diperbarui secara berkala.
4. **Network Traffic Terkalibrasi:**
   - Mengontrol ukuran dan layout indikator kecepatan transfer data bawaan ROM agar pas berdampingan dengan ikon baterai.
5. **Aplikasi Pengaturan Lengkap (Settings UI):**
   - Mengatur format jam (detik/tanpa detik, 12h/24h), format tanggal, posisi tanggal (center/start), bentuk suhu (1 baris/2 baris), skala ikon, dll.
   - Dilengkapi tombol **"⚡ Terapkan & Restart SystemUI"** untuk menerapkan konfigurasi dalam sekali klik.

---

## 🛠️ Struktur Project

```text
DualLineStatusBar/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Scope LSPosed: com.android.systemui
│   │   ├── assets/
│   │   │   └── xposed_init           # Entry point modul
│   │   ├── java/com/dualline/statusbar/
│   │   │   ├── ModulePrefs.java      # Manajemen preferensi terpusat
│   │   │   ├── hooks/
│   │   │   │   ├── MainHook.java     # Router IXposedHookLoadPackage
│   │   │   │   ├── StatusBarHook.java# Penataan tata letak kiri & kanan dual-line
│   │   │   │   ├── ClockHook.java    # Hook teks jam & detik
│   │   │   │   ├── NetworkTrafficHook.java # Hook traffic meter
│   │   │   │   └── NotificationHook.java   # Hook notifikasi
│   │   │   └── ui/
│   │   │       ├── SettingsActivity.java   # UI pengaturan manual
│   │   │       ├── ThermalView.java        # Widget suhu CPU & Baterai
│   │   │       └── DateView.java           # Widget tanggal otomatis
│   │   └── res/
│   │       ├── layout/activity_settings.xml# Layout antarmuka pengaturan
│   │       ├── values/strings.xml
│   │       └── xml/
│   │           ├── module_prefs.xml
│   │           └── xposed_scope.xml
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 📦 Pemasangan & Persyaratan

- **Android:** Android 12 (API 31) hingga Android 17 (API 35+)
- **Framework:** LSPosed (Zygisk / Riru)
- **Root:** Magisk / KernelSU / APatch

### Langkah Pemasangan:
1. Unduh dan install file APK release (`DualLineStatusBar-release.apk`).
2. Buka aplikasi **LSPosed Manager**.
3. Aktifkan modul **DualLineStatusBar** dan pastikan scope **System Framework** / **System UI** dicentang.
4. Restart SystemUI atau reboot perangkat.
5. Buka aplikasi **DualLine StatusBar** untuk menyesuaikan pengaturan sesuai keinginan Anda.
