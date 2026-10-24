package com.dualline.statusbar.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DateView.java - Widget Tanggal dinamis untuk Baris 2 (kiri) Status Bar
 *
 * Mengikuti konfigurasi pengguna dari ModulePrefs (format, ukuran, perataan, tebal hari).
 */
public class DateView extends TextView {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsAttached = false;

    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDate();
        }
    };

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsAttached) return;
            updateDate();
            long now = System.currentTimeMillis();
            long nextMinute = ((now / 60000L) + 1L) * 60000L;
            mHandler.postDelayed(this, nextMinute - now);
        }
    };

    public DateView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        applyPreferences();

        setIncludeFontPadding(false);
        setSingleLine(true);
        setMaxLines(1);
        setPadding(0, 0, 0, 0);
        setTextColor(Color.WHITE);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setLayoutParams(lp);

        updateDate();
    }

    public void applyPreferences() {
        float density = getResources().getDisplayMetrics().density;
        boolean showDate = ModulePrefs.getBoolean(ModulePrefs.KEY_DATE_SHOW, true);
        if (!showDate) {
            setVisibility(View.GONE);
            return;
        } else {
            setVisibility(View.VISIBLE);
        }

        float sizeDp = ModulePrefs.getFloat(ModulePrefs.KEY_DATE_SIZE_DP, 6.2f);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeDp * density);

        String alignment = ModulePrefs.getString(ModulePrefs.KEY_DATE_ALIGNMENT, "center");
        if ("start".equalsIgnoreCase(alignment)) {
            setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        } else {
            setGravity(Gravity.CENTER);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        applyPreferences();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        try {
            getContext().registerReceiver(mTimeReceiver, filter);
        } catch (Throwable ignored) {}
        mHandler.post(mUpdateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttached = false;
        mHandler.removeCallbacks(mUpdateRunnable);
        try {
            getContext().unregisterReceiver(mTimeReceiver);
        } catch (Throwable ignored) {}
    }

    private void updateDate() {
        boolean showDate = ModulePrefs.getBoolean(ModulePrefs.KEY_DATE_SHOW, true);
        if (!showDate) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        String format = ModulePrefs.getString(ModulePrefs.KEY_DATE_FORMAT, "EEE, dd MMM");
        boolean boldDay = ModulePrefs.getBoolean(ModulePrefs.KEY_DATE_BOLD_DAY, true);

        try {
            String text = new SimpleDateFormat(format, Locale.getDefault()).format(new Date());
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            if (boldDay) {
                int delimIdx = text.indexOf(',');
                if (delimIdx < 0) delimIdx = text.indexOf(' ');
                if (delimIdx > 0) {
                    ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, delimIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            setText(ssb);
        } catch (Throwable ignored) {
            setText("--");
        }
    }

    public void setTintColor(int color) {
        setTextColor(color);
    }
}
