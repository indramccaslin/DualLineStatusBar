package com.dualline.statusbar.ui;

import android.os.Environment;
import android.os.StatFs;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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

import java.io.File;

/**
 * StorageView.java - Widget Info Storage (Internal Memory) untuk Baris 1 Kanan (Paling Kiri)
 *
 * Menampilkan persentase / sisa memori internal terpakai (mis. "ROM 52%") di sebelah kiri Baris 1 Kanan.
 */
public class StorageView extends TextView {

    private static final long UPDATE_INTERVAL_MS = 15000L; // update tiap 15 detik

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsAttached = false;

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsAttached) return;
            updateDisplay();
            mHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    public StorageView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        applyPreferences();

        setIncludeFontPadding(false);
        setSingleLine(true);
        setMaxLines(1);
        setLines(1);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(0, 0, 0, 0);
        setTextColor(Color.WHITE);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setLayoutParams(lp);

        updateDisplay();
    }

    public void applyPreferences() {
        float density = getResources().getDisplayMetrics().density;
        boolean showStorage = ModulePrefs.getBoolean(ModulePrefs.KEY_STORAGE_SHOW, true);
        setVisibility(showStorage ? View.VISIBLE : View.GONE);
        if (!showStorage) return;

        float sizeDp = ModulePrefs.getFloat(ModulePrefs.KEY_THERMAL_SIZE_DP, 5.8f);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeDp * density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        applyPreferences();
        mHandler.post(mUpdateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttached = false;
        mHandler.removeCallbacks(mUpdateRunnable);
    }

    public void updateDisplay() {
        if (!ModulePrefs.getBoolean(ModulePrefs.KEY_STORAGE_SHOW, true)) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        int percent = readStorageUsedPercent();
        String text = (percent >= 0) ? ("ROM " + percent + "%") : "ROM --%";

        // Tambah spasi di belakang agar ada jarak dengan ikon Wi-Fi di sebelah kanannya (2 spasi)
        text = text + "  ";

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(ssb);
    }

    public void setTintColor(int color) {
        setTextColor(color);
    }

    private int readStorageUsedPercent() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            long total = totalBlocks * blockSize;
            long available = availableBlocks * blockSize;
            long used = total - available;

            if (total > 0) {
                int percent = Math.round(used * 100f / total);
                return Math.max(0, Math.min(100, percent));
            }
        } catch (Throwable ignored) {}
        return -1;
    }
}
