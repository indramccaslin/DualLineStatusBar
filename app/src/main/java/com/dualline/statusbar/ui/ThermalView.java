package com.dualline.statusbar.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dualline.statusbar.ModulePrefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * ThermalView.java - Widget Suhu CPU & Baterai untuk Baris 1 Status Bar
 *
 * Mengikuti preferensi pengguna (ModulePrefs) untuk visibilitas, format
 * (1 baris / 2 baris / cpu only / bat only), jumlah spasi, ukuran font, dan interval update.
 */
public class ThermalView extends TextView {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private File mCpuTempFile = null;
    private File mBatTempFile = null;
    private int mBatteryTemp = -1;
    private boolean mIsAttached = false;

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                if (temp > 0) {
                    mBatteryTemp = Math.round(temp / 10.0f);
                    updateDisplay();
                }
            }
        }
    };

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsAttached) return;
            updateDisplay();
            int intervalSec = ModulePrefs.getInt(ModulePrefs.KEY_THERMAL_INTERVAL, 2);
            if (intervalSec < 1) intervalSec = 2;
            mHandler.postDelayed(this, intervalSec * 1000L);
        }
    };

    public ThermalView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        applyPreferences();

        setIncludeFontPadding(false);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(0, 0, 0, 0);
        setTextColor(Color.WHITE);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setLayoutParams(lp);

        findThermalFiles();
        updateDisplay();
    }

    public void applyPreferences() {
        float density = getResources().getDisplayMetrics().density;
        boolean showThermal = ModulePrefs.getBoolean(ModulePrefs.KEY_THERMAL_SHOW, true);
        if (!showThermal) {
            setVisibility(View.GONE);
            return;
        } else {
            setVisibility(View.VISIBLE);
        }

        String format = ModulePrefs.getString(ModulePrefs.KEY_THERMAL_FORMAT, "single_line");
        float sizeDp = ModulePrefs.getFloat(ModulePrefs.KEY_THERMAL_SIZE_DP, 5.8f);

        if ("two_lines".equalsIgnoreCase(format)) {
            setSingleLine(false);
            setMaxLines(2);
            setLines(2);
            setLineSpacing(0f, 0.75f);
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 4.8f * density);
        } else {
            setSingleLine(true);
            setMaxLines(1);
            setLines(1);
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeDp * density);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        applyPreferences();
        try {
            getContext().registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Throwable ignored) {}
        mHandler.post(mUpdateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttached = false;
        mHandler.removeCallbacks(mUpdateRunnable);
        try {
            getContext().unregisterReceiver(mBatteryReceiver);
        } catch (Throwable ignored) {}
    }

    /**
     * Memperbarui teks suhu CPU dan Baterai sesuai format preferensi
     */
    public void updateDisplay() {
        boolean showThermal = ModulePrefs.getBoolean(ModulePrefs.KEY_THERMAL_SHOW, true);
        if (!showThermal) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        int cpuTemp = readCpuTemp();
        int batTemp = (mBatteryTemp > 0) ? mBatteryTemp : readBatTemp();

        String format = ModulePrefs.getString(ModulePrefs.KEY_THERMAL_FORMAT, "single_line");
        int numSpaces = ModulePrefs.getInt(ModulePrefs.KEY_THERMAL_SPACING, 2);
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < Math.max(1, numSpaces); i++) spaces.append(" ");

        String cpuStr = "CPU " + ((cpuTemp > 0) ? (cpuTemp + "°") : "--°");
        String batStr = "BAT " + ((batTemp > 0) ? (batTemp + "°") : "--°");

        SpannableStringBuilder ssb;

        if ("cpu_only".equalsIgnoreCase(format)) {
            ssb = new SpannableStringBuilder(cpuStr);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if ("bat_only".equalsIgnoreCase(format)) {
            ssb = new SpannableStringBuilder(batStr);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if ("two_lines".equalsIgnoreCase(format)) {
            String full = cpuStr + "\n" + batStr;
            ssb = new SpannableStringBuilder(full);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            int batStart = cpuStr.length() + 1;
            ssb.setSpan(new StyleSpan(Typeface.BOLD), batStart, batStart + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            // single_line (default)
            String full = cpuStr + spaces.toString() + batStr;
            ssb = new SpannableStringBuilder(full);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            int batStart = cpuStr.length() + spaces.length();
            ssb.setSpan(new StyleSpan(Typeface.BOLD), batStart, batStart + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        setText(ssb);
    }

    public void setTintColor(int color) {
        setTextColor(color);
    }

    private int readCpuTemp() {
        if (mCpuTempFile != null && mCpuTempFile.exists()) {
            int val = readIntFromFile(mCpuTempFile);
            if (val > 0) return val;
        }
        findThermalFiles();
        if (mCpuTempFile != null) {
            return readIntFromFile(mCpuTempFile);
        }
        return -1;
    }

    private int readBatTemp() {
        if (mBatTempFile != null && mBatTempFile.exists()) {
            int val = readIntFromFile(mBatTempFile);
            if (val > 0) return val;
        }
        return -1;
    }

    private int readIntFromFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line != null) {
                long val = Long.parseLong(line.trim());
                if (val > 1000) val /= 1000;
                if (val > 0 && val < 120) {
                    return (int) val;
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private void findThermalFiles() {
        if (mCpuTempFile != null && mBatTempFile != null) return;

        String[] cpuTargets = {"cpuss-0", "cpuss-1", "cpu-1-7", "cpu-0-0", "soc"};
        String[] batTargets = {"battery", "bms", "pm8150b_tz"};

        for (int i = 0; i <= 95; i++) {
            File typeFile = new File("/sys/class/thermal/thermal_zone" + i + "/type");
            File tempFile = new File("/sys/class/thermal/thermal_zone" + i + "/temp");
            if (!typeFile.exists() || !tempFile.exists()) continue;

            try (BufferedReader br = new BufferedReader(new FileReader(typeFile))) {
                String type = br.readLine();
                if (type == null) continue;
                String typeLower = type.trim().toLowerCase();

                if (mCpuTempFile == null) {
                    for (String target : cpuTargets) {
                        if (typeLower.contains(target)) {
                            mCpuTempFile = tempFile;
                            break;
                        }
                    }
                }

                if (mBatTempFile == null) {
                    for (String target : batTargets) {
                        if (typeLower.contains(target)) {
                            mBatTempFile = tempFile;
                            break;
                        }
                    }
                }

                if (mCpuTempFile != null && mBatTempFile != null) break;

            } catch (Throwable ignored) {}
        }
    }
}
