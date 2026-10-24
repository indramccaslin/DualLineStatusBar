package com.dualline.statusbar.hooks;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import com.dualline.statusbar.ModulePrefs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * ClockHook.java - Hook untuk Clock widget di SystemUI status bar
 *
 * Mengatur tampilan jam di Line 1 sesuai preferensi pengguna (ModulePrefs).
 */
public class ClockHook {

    private static final String TAG = "DualLineStatusBar/Clock";
    private static final String CLOCK_CLASS = "com.android.systemui.statusbar.policy.Clock";

    public void initHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clockClass = XposedHelpers.findClass(CLOCK_CLASS, lpparam.classLoader);
            XposedBridge.log(TAG + ": Ditemukan class " + CLOCK_CLASS);

            // Hook getSmallTime(): kembalikan teks jam (Line 1)
            XposedHelpers.findAndHookMethod(
                clockClass,
                "getSmallTime",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (!(param.thisObject instanceof TextView)) return;
                            TextView clockView = (TextView) param.thisObject;

                            if (!ModulePrefs.getBoolean(ModulePrefs.KEY_DUAL_LINE_ENABLED, true)) return;

                            boolean showSeconds = ModulePrefs.getBoolean(ModulePrefs.KEY_CLOCK_SHOW_SECONDS, true);
                            String formatJam = ModulePrefs.getString(ModulePrefs.KEY_CLOCK_TIME_FORMAT, "HH:mm:ss");
                            if (!showSeconds && formatJam.contains(":ss")) {
                                formatJam = formatJam.replace(":ss", "");
                            }

                            float sizeFactor = ModulePrefs.getFloat(ModulePrefs.KEY_CLOCK_SIZE_FACTOR, 0.75f);
                            boolean isBold = ModulePrefs.getBoolean(ModulePrefs.KEY_CLOCK_BOLD, true);

                            SpannableStringBuilder ssb = buatTeksJam(formatJam, sizeFactor, isBold);

                            // Layout single-line untuk Line 1
                            clockView.setSingleLine(true);
                            clockView.setMaxLines(1);
                            clockView.setLines(1);
                            clockView.setIncludeFontPadding(false);
                            clockView.setGravity(android.view.Gravity.CENTER);

                            param.setResult(ssb);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error di getSmallTime: " + t.getMessage());
                        }
                    }
                }
            );
            XposedBridge.log(TAG + ": Hook Clock.getSmallTime() BERHASIL!");

            // Hook onAttachedToWindow: atur single-line & update detik
            XposedHelpers.findAndHookMethod(
                clockClass,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (!(param.thisObject instanceof TextView)) return;
                            TextView clockView = (TextView) param.thisObject;
                            clockView.setSingleLine(true);
                            clockView.setMaxLines(1);
                            clockView.setLines(1);
                            clockView.setIncludeFontPadding(false);
                            clockView.setGravity(android.view.Gravity.CENTER);

                            boolean showSeconds = ModulePrefs.getBoolean(ModulePrefs.KEY_CLOCK_SHOW_SECONDS, true);
                            try {
                                XposedHelpers.setBooleanField(clockView, "mShowSeconds", showSeconds);
                                XposedHelpers.callMethod(clockView, "updateShowSeconds");
                            } catch (Throwable ignored) {}
                        } catch (Throwable ignored) {}
                    }
                }
            );
            XposedBridge.log(TAG + ": Hook Clock.onAttachedToWindow() BERHASIL!");

            // Hook updateShowSeconds: patuhi preferensi user
            try {
                XposedHelpers.findAndHookMethod(clockClass, "updateShowSeconds",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                boolean showSeconds = ModulePrefs.getBoolean(ModulePrefs.KEY_CLOCK_SHOW_SECONDS, true);
                                XposedHelpers.setBooleanField(param.thisObject, "mShowSeconds", showSeconds);
                            } catch (Throwable ignored) {}
                        }
                    }
                );
                XposedBridge.log(TAG + ": Hook Clock.updateShowSeconds() BERHASIL!");
            } catch (Throwable ignored) {}

        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(TAG + ": Class " + CLOCK_CLASS + " tidak ditemukan: " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error saat hook Clock: " + t.getMessage());
        }
    }

    /**
     * Membuat teks jam sesuai format, ukuran, dan ketebalan dari preferensi
     */
    private SpannableStringBuilder buatTeksJam(String formatJam, float sizeFactor, boolean isBold) {
        String teksJam;
        try {
            teksJam = new SimpleDateFormat(formatJam, Locale.getDefault()).format(new Date());
        } catch (Exception e) {
            teksJam = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(teksJam);
        ssb.setSpan(new RelativeSizeSpan(sizeFactor), 0, teksJam.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isBold) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, teksJam.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }
}
