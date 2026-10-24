package com.dualline.statusbar.hooks;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * NetworkTrafficHook.java - Penata Network Traffic Kompak & Pas dengan Baterai
 *
 * Mengunci ukuran teks Network Traffic pada level framework & SystemUI:
 * 1. Menghook TextView.setTextSize() khusus untuk instance NetworkTraffic agar tidak pernah membesar (dikunci pada 4.0dp).
 * 2. Mengubah mTextSize pada com.android.internal.statusbar.NetworkTraffic agar displayStatsAndReschedule() tidak mereset ke ukuran besar.
 * 3. Menghook displayStatsAndReschedule() pada NetworkTraffic$1 agar selalu menerapkan ukuran 4.0dp dan spasi rapat 0.70f.
 * 4. Memastikan format 2 baris (Angka di atas, Satuan di bawah) pas atas-bawah dan tidak terpotong.
 */
public class NetworkTrafficHook {

    private static final String TAG = "DualLineStatusBar/NetworkTraffic";
    private static final String INTERNAL_NETWORK_TRAFFIC = "com.android.internal.statusbar.NetworkTraffic";
    private static final String INTERNAL_TRAFFIC_HANDLER = "com.android.internal.statusbar.NetworkTraffic$1";
    private static final String SYSTEMUI_NETWORK_TRAFFIC = "com.android.systemui.statusbar.NetworkTraffic";

    public void initHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Hook TextView.setTextSize(int unit, float size) secara global untuk semua class NetworkTraffic
        try {
            XposedHelpers.findAndHookMethod(
                TextView.class,
                "setTextSize",
                int.class,
                float.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String className = param.thisObject.getClass().getName();
                            if (className.contains("NetworkTraffic")) {
                                TextView tv = (TextView) param.thisObject;
                                float density = tv.getResources().getDisplayMetrics().density;
                                float smallTextPx = 5.2f * density;

                                // Kunci pada ukuran 5.2dp agar pas mengisi ruang baris dan setinggi baterai
                                param.args[0] = TypedValue.COMPLEX_UNIT_PX;
                                param.args[1] = smallTextPx;

                                tv.setIncludeFontPadding(false);
                                tv.setLineSpacing(0f, 0.75f);
                                tv.setGravity(Gravity.CENTER);
                                tv.setPadding(0, 0, 0, 0);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            );
            XposedBridge.log(TAG + ": Hook TextView.setTextSize untuk NetworkTraffic BERHASIL!");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Gagal hook TextView.setTextSize: " + t.getMessage());
        }

        // 2. Hook com.android.internal.statusbar.NetworkTraffic
        try {
            Class<?> internalNetTraffic = XposedHelpers.findClass(INTERNAL_NETWORK_TRAFFIC, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                internalNetTraffic,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        configureInternalTraffic((TextView) param.thisObject);
                    }
                }
            );

            try {
                XposedHelpers.findAndHookMethod(
                    internalNetTraffic,
                    "updateSettings",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            configureInternalTraffic((TextView) param.thisObject);
                        }
                    }
                );
            } catch (Throwable ignored) {}

            try {
                XposedHelpers.findAndHookMethod(
                    internalNetTraffic,
                    "updateViewState",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            configureInternalTraffic((TextView) param.thisObject);
                        }
                    }
                );
            } catch (Throwable ignored) {}

            XposedBridge.log(TAG + ": Hook com.android.internal.statusbar.NetworkTraffic BERHASIL!");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Gagal hook internal NetworkTraffic: " + t.getMessage());
        }

        // 3. Hook com.android.internal.statusbar.NetworkTraffic$1 (Handler)
        try {
            Class<?> internalHandler = XposedHelpers.findClass(INTERNAL_TRAFFIC_HANDLER, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                internalHandler,
                "displayStatsAndReschedule",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object trafficView = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            if (trafficView instanceof TextView) {
                                configureInternalTraffic((TextView) trafficView);
                            }
                        } catch (Throwable ignored) {}
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object trafficView = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            if (trafficView instanceof TextView) {
                                configureInternalTraffic((TextView) trafficView);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            );

            XposedBridge.log(TAG + ": Hook com.android.internal.statusbar.NetworkTraffic$1 BERHASIL!");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Gagal hook internal NetworkTraffic$1: " + t.getMessage());
        }

        // 4. Hook com.android.systemui.statusbar.NetworkTraffic (cadangan bila ROM memakai class ini)
        try {
            Class<?> sysUiNetTraffic = XposedHelpers.findClass(SYSTEMUI_NETWORK_TRAFFIC, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                sysUiNetTraffic,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        configureInternalTraffic((TextView) param.thisObject);
                    }
                }
            );

            try {
                XposedHelpers.findAndHookMethod(
                    sysUiNetTraffic,
                    "setFixedWidth",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            configureInternalTraffic((TextView) param.thisObject);
                        }
                    }
                );
            } catch (Throwable ignored) {}

            XposedBridge.log(TAG + ": Hook com.android.systemui.statusbar.NetworkTraffic BERHASIL!");
        } catch (Throwable ignored) {}
    }

    /**
     * Mengatur konfigurasi NetworkTraffic TextView agar proporsional,
     * tidak membesar sendiri, dan pas dengan tinggi baterai.
     */
    public static void configureInternalTraffic(TextView tv) {
        if (tv == null) return;
        try {
            float density = tv.getResources().getDisplayMetrics().density;
            int smallTextPx = Math.round(5.2f * density); // ~14.3px (~5.2dp)

            // Kunci mTextSize di dalam instance agar displayStatsAndReschedule() memakai ukuran ini
            try {
                XposedHelpers.setIntField(tv, "mTextSize", smallTextPx);
            } catch (Throwable ignored) {}

            // Set text size langsung
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallTextPx);

            // Hilangkan font padding atas-bawah
            tv.setIncludeFontPadding(false);

            // Rapatkan jarak antar baris
            tv.setLineSpacing(0f, 0.75f);

            // Pastikan mode 2 baris
            tv.setSingleLine(false);
            tv.setMaxLines(2);
            tv.setLines(2);

            // Rata tengah
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 0, 0, 0);

            // Hilangkan compound drawables (panah)
            tv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

            // Lebar dan tinggi dinamis
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp != null) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                tv.setLayoutParams(lp);
            }
            tv.setMinimumWidth(0);
            tv.setMinimumHeight(0);

            // Pastikan parent container tidak memotong teks
            if (tv.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) tv.getParent();
                parent.setClipChildren(false);
                parent.setClipToPadding(false);
                parent.setPadding(0, 0, 0, 0);
                ViewGroup.LayoutParams parentLp = parent.getLayoutParams();
                if (parentLp != null) {
                    parentLp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    parentLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    parent.setLayoutParams(parentLp);
                }
            }

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error configureInternalTraffic: " + t.getMessage());
        }
    }

    public static void configureNetworkTraffic(TextView tv) {
        configureInternalTraffic(tv);
    }
}
