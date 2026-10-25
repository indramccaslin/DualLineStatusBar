package com.dualline.statusbar.ui;

import android.app.ActivityManager;
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

/**
 * MemView.java - Widget Info RAM untuk Baris 1 Status Bar
 *
 * Menampilkan persentase RAM terpakai (mis. "RAM 47%") di sebelah info suhu.
 * Nilai dibaca dari ActivityManager.MemoryInfo dan diperbarui tiap 5 detik.
 */
public class MemView extends TextView {

    private static final long UPDATE_INTERVAL_MS = 5000L;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ActivityManager.MemoryInfo mMemInfo = new ActivityManager.MemoryInfo();
    private ActivityManager mActivityManager;
    private boolean mIsAttached = false;

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsAttached) return;
            updateDisplay();
            mHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    public MemView(Context context) {
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

        mActivityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        updateDisplay();
    }

    public void applyPreferences() {
        float density = getResources().getDisplayMetrics().density;
        boolean showMem = ModulePrefs.getBoolean(ModulePrefs.KEY_MEM_SHOW, true);
        setVisibility(showMem ? View.VISIBLE : View.GONE);
        if (!showMem) return;

        // Samakan ukuran font dengan info suhu agar baris 1 tetap rapi.
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

    /**
     * Memperbarui teks persentase RAM terpakai.
     */
    public void updateDisplay() {
        if (!ModulePrefs.getBoolean(ModulePrefs.KEY_MEM_SHOW, true)) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        int percent = readUsedPercent();
        String text = (percent >= 0) ? ("RAM " + percent + "%") : "RAM --%";

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(ssb);
    }

    public void setTintColor(int color) {
        setTextColor(color);
    }

    private int readUsedPercent() {
        try {
            if (mActivityManager != null) {
                mActivityManager.getMemoryInfo(mMemInfo);
                long total = mMemInfo.totalMem;
                long avail = mMemInfo.availMem;
                if (total > 0) {
                    long used = total - avail;
                    int percent = Math.round(used * 100f / total);
                    return Math.max(0, Math.min(100, percent));
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }
}
