package com.dualline.statusbar;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;

/**
 * ModulePrefs.java - Manajemen Terpusat Konfigurasi DualLineStatusBar
 *
 * Menghubungkan antarmuka pengaturan (SettingsActivity) dengan hook SystemUI
 * melalui XSharedPreferences LSPosed dan izin file lintas proses.
 */
public class ModulePrefs {

    public static final String PACKAGE_NAME = "com.dualline.statusbar";
    public static final String PREFS_NAME = "dualline_prefs";

    // === 1. MASTER ===
    public static final String KEY_DUAL_LINE_ENABLED = "pref_dual_line_enabled";

    // === 2. JAM (LINE 1 KIRI) ===
    public static final String KEY_CLOCK_SHOW_SECONDS = "pref_clock_show_seconds";
    public static final String KEY_CLOCK_TIME_FORMAT = "pref_clock_time_format";
    public static final String KEY_CLOCK_BOLD = "pref_clock_bold";
    public static final String KEY_CLOCK_SIZE_FACTOR = "pref_clock_size_factor"; // float: 0.65f, 0.75f, 0.85f

    // === 3. TANGGAL (LINE 2 KIRI) ===
    public static final String KEY_DATE_SHOW = "pref_date_show";
    public static final String KEY_DATE_FORMAT = "pref_date_format";
    public static final String KEY_DATE_ALIGNMENT = "pref_date_alignment"; // "center" atau "start"
    public static final String KEY_DATE_SIZE_DP = "pref_date_size_dp";     // float: 5.2f, 6.2f, 7.2f
    public static final String KEY_DATE_BOLD_DAY = "pref_date_bold_day";

    // === 4. NOTIFIKASI (LINE 2 KIRI) ===
    public static final String KEY_NOTIF_SHOW = "pref_notif_show";
    public static final String KEY_NOTIF_SCALE = "pref_notif_scale";       // float: 0.60f, 0.70f, 0.80f

    // === 5. SUHU CPU & BATERAI (LINE 1 KANAN) ===
    public static final String KEY_THERMAL_SHOW = "pref_thermal_show";
    public static final String KEY_THERMAL_FORMAT = "pref_thermal_format"; // "single_line", "two_lines", "cpu_only", "bat_only"
    public static final String KEY_THERMAL_SPACING = "pref_thermal_spacing"; // int: 1, 2, 3
    public static final String KEY_THERMAL_INTERVAL = "pref_thermal_interval"; // int: 1, 2, 5
    public static final String KEY_THERMAL_SIZE_DP = "pref_thermal_size_dp"; // float: 5.0f, 5.8f, 6.5f

    // === 5b. INFO RAM (LINE 1) ===
    public static final String KEY_MEM_SHOW = "pref_mem_show";

    // === 6. NETWORK TRAFFIC & STATUS ICONS (LINE 2 & LINE 1 KANAN) ===
    public static final String KEY_NET_TRAFFIC_SHOW = "pref_net_traffic_show";
    public static final String KEY_BATTERY_SCALE = "pref_battery_scale";   // float: 0.70f, 0.80f, 1.0f
    public static final String KEY_STATUS_ICONS_SCALE = "pref_status_icons_scale"; // float: 0.70f, 0.80f, 1.0f

    private static XSharedPreferences sXPrefs = null;

    public static XSharedPreferences getXPrefs() {
        if (sXPrefs == null) {
            sXPrefs = new XSharedPreferences(PACKAGE_NAME, PREFS_NAME);
            sXPrefs.makeWorldReadable();
        } else {
            sXPrefs.reload();
        }
        return sXPrefs;
    }

    public static boolean getBoolean(String key, boolean defValue) {
        try {
            XSharedPreferences xPrefs = getXPrefs();
            if (xPrefs != null) {
                return xPrefs.getBoolean(key, defValue);
            }
        } catch (Throwable ignored) {}
        return defValue;
    }

    public static String getString(String key, String defValue) {
        try {
            XSharedPreferences xPrefs = getXPrefs();
            if (xPrefs != null) {
                return xPrefs.getString(key, defValue);
            }
        } catch (Throwable ignored) {}
        return defValue;
    }

    public static float getFloat(String key, float defValue) {
        try {
            XSharedPreferences xPrefs = getXPrefs();
            if (xPrefs != null) {
                return xPrefs.getFloat(key, defValue);
            }
        } catch (Throwable ignored) {}
        return defValue;
    }

    public static int getInt(String key, int defValue) {
        try {
            XSharedPreferences xPrefs = getXPrefs();
            if (xPrefs != null) {
                return xPrefs.getInt(key, defValue);
            }
        } catch (Throwable ignored) {}
        return defValue;
    }

    /**
     * Memperbaiki izin file SharedPrefs agar SystemUI dapat membaca konfigurasi tanpa hambatan
     */
    public static void fixPermissions(Context context) {
        try {
            File dataDir = context.getDataDir();
            File prefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, PREFS_NAME + ".xml");

            dataDir.setReadable(true, false);
            dataDir.setExecutable(true, false);
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false);
                prefsDir.setExecutable(true, false);
            }
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {}

        // Root chmod untuk keamanan ekstra lintas ROM
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                    "chmod 755 /data/user/0/com.dualline.statusbar /data/user/0/com.dualline.statusbar/shared_prefs 2>/dev/null; " +
                    "chmod 666 /data/user/0/com.dualline.statusbar/shared_prefs/dualline_prefs.xml 2>/dev/null"});
                p.waitFor();
            } catch (Throwable ignored) {}
        }).start();
    }
}
