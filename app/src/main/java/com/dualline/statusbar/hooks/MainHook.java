package com.dualline.statusbar.hooks;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * MainHook.java - Entry point utama modul Xposed DualLineStatusBar
 *
 * Kelas ini adalah titik masuk pertama yang dipanggil oleh framework LSPosed
 * ketika package yang di-scope (com.android.systemui) dimuat.
 *
 * Cara kerja:
 * 1. LSPosed membaca file assets/xposed_init
 * 2. LSPosed menemukan nama kelas ini di xposed_init
 * 3. LSPosed memanggil handleLoadPackage() saat SystemUI dimuat
 * 4. Kita kemudian mengaktifkan semua hook yang diperlukan
 *
 * @author DualLineStatusBar Module
 * @version 1.0.0
 */
public class MainHook implements IXposedHookLoadPackage {

    // Tag untuk logging di XposedBridge log
    private static final String TAG = "DualLineStatusBar";

    // Package SystemUI yang akan kita hook
    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    /**
     * Dipanggil oleh framework Xposed setiap kali sebuah package dimuat ke memori.
     * Kita hanya memproses package com.android.systemui.
     *
     * @param lpparam Parameter yang berisi informasi tentang package yang sedang dimuat,
     *                termasuk ClassLoader dan nama package
     */
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hanya proses jika package yang dimuat adalah SystemUI
        if (!PACKAGE_SYSTEMUI.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": SystemUI terdeteksi, mengaktifkan hooks...");

        try {
            // Aktifkan hook untuk status bar utama (mengubah tinggi dan layout)
            StatusBarHook statusBarHook = new StatusBarHook();
            statusBarHook.initHooks(lpparam);
            XposedBridge.log(TAG + ": StatusBarHook berhasil diinisialisasi");
        } catch (Throwable t) {
            // Catat error tapi jangan crash - hook lain mungkin masih bisa berjalan
            XposedBridge.log(TAG + ": Gagal menginisialisasi StatusBarHook: " + t.getMessage());
            XposedBridge.log(t);
        }

        try {
            // Aktifkan hook untuk Clock widget (menampilkan jam + tanggal dual-line)
            ClockHook clockHook = new ClockHook();
            clockHook.initHooks(lpparam);
            XposedBridge.log(TAG + ": ClockHook berhasil diinisialisasi");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Gagal menginisialisasi ClockHook: " + t.getMessage());
            XposedBridge.log(t);
        }

        try {
            // Aktifkan hook untuk NetworkTraffic widget (2 baris kompak, pas dengan baterai)
            NetworkTrafficHook networkTrafficHook = new NetworkTrafficHook();
            networkTrafficHook.initHooks(lpparam);
            XposedBridge.log(TAG + ": NetworkTrafficHook berhasil diinisialisasi");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Gagal menginisialisasi NetworkTrafficHook: " + t.getMessage());
            XposedBridge.log(t);
        }

        XposedBridge.log(TAG + ": Semua hooks telah diinisialisasi untuk " + lpparam.packageName);
    }
}
