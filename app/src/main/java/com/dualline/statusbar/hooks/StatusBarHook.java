package com.dualline.statusbar.hooks;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dualline.statusbar.ModulePrefs;
import com.dualline.statusbar.ui.DateView;
import com.dualline.statusbar.ui.MemView;
import com.dualline.statusbar.ui.ThermalView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * StatusBarHook.java - Penata Status Bar Kanan (Dual Line)
 *
 * Mengatur sisi kanan status bar menjadi 2 baris proporsional:
 * - Baris 1 (Atas):  Sinyal Seluler -> Wi-Fi (StatusIconContainer)
 * - Baris 2 (Bawah): Network Traffic (Kiri) -> Baterai (Kanan)
 *
 * Mengembalikan tampilan asli NetworkTraffic bawaan ROM dengan skala yang diperkecil agar pas dan tidak terpotong.
 */
public class StatusBarHook {

    private static final String TAG = "DualLineStatusBar/StatusBar";
    public void initHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> phoneStatusBarViewClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(
                phoneStatusBarViewClass,
                "onFinishInflate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            View statusBarView = (View) param.thisObject;
                            Context context = statusBarView.getContext();

                            if (!ModulePrefs.getBoolean(ModulePrefs.KEY_DUAL_LINE_ENABLED, true)) return;

                            statusBarView.postDelayed(() -> {
                                susunSisiKiriDuaBaris(statusBarView, context);
                                susunSisiKananDuaBaris(statusBarView, context);
                            }, 100);

                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error di PhoneStatusBarView onFinishInflate: " + t.getMessage());
                        }
                    }
                }
            );

            XposedBridge.log(TAG + ": Hook PhoneStatusBarView.onFinishInflate BERHASIL dipasang!");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Gagal hook PhoneStatusBarView: " + t.getMessage());
        }
    }

    /**
     * Menyusun sisi kiri:
     * - Line 1: Jam (HH:mm:ss)
     * - Line 2: Tanggal (EEE, dd MMM) di kiri/tengah + Ikon Notifikasi di sebelah kanannya
     *
     * Jam dan Tanggal diisolasi dalam leftStack (kolom tersendiri) sehingga posisinya
     * 100% stabil dan tidak bergerak ketika ada notifikasi masuk/keluar.
     */
    private void susunSisiKiriDuaBaris(View statusBarView, Context context) {
        try {
            if (statusBarView.findViewWithTag("dualline_left_root") != null) return;

            // 1. Cari Clock view (kiri, bukan tengah/kanan)
            View clockView = findViewByFilter(statusBarView, v -> {
                String cls = v.getClass().getSimpleName();
                String res = getResourceEntryName(v);
                return (cls.contains("Clock") || res.equals("clock")) &&
                       !res.contains("center") && !res.contains("right");
            });

            if (clockView == null) {
                XposedBridge.log(TAG + ": Clock tidak ditemukan, skip sisi kiri.");
                return;
            }

            // 2. Cari container notifikasi bawaan ROM
            View notifContainer = findViewByFilter(statusBarView, v -> {
                String res = getResourceEntryName(v);
                return res.contains("start_side_notif") || res.contains("notification_icon_area");
            });

            // Parent horizontal LinearLayout dari Clock
            ViewGroup horzLinear = (clockView.getParent() instanceof ViewGroup)
                ? (ViewGroup) clockView.getParent() : null;
            // FrameLayout grandparent
            ViewGroup frameParent = (horzLinear != null && horzLinear.getParent() instanceof ViewGroup)
                ? (ViewGroup) horzLinear.getParent() : null;

            if (frameParent == null) {
                XposedBridge.log(TAG + ": frameParent sisi kiri tidak ditemukan.");
                return;
            }

            // Hilangkan clipping di chain agar konten tidak terpotong
            ViewGroup chain = frameParent;
            while (chain != null && chain != statusBarView) {
                chain.setClipChildren(false);
                chain.setClipToPadding(false);
                chain = (chain.getParent() instanceof ViewGroup) ? (ViewGroup) chain.getParent() : null;
            }
            if (statusBarView instanceof ViewGroup) {
                ((ViewGroup) statusBarView).setClipChildren(false);
                ((ViewGroup) statusBarView).setClipToPadding(false);
            }

            statusBarView.setTag("dualline_left_root");
            float density = context.getResources().getDisplayMetrics().density;

            // Lepas Clock dari horizontal LinearLayout aslinya
            horzLinear.removeView(clockView);

            // Lepas notifContainer dari posisi aslinya
            if (notifContainer != null && notifContainer.getParent() instanceof ViewGroup) {
                ((ViewGroup) notifContainer.getParent()).removeView(notifContainer);
            }

            // ── leftRoot: HORIZONTAL, menampung [ leftStack | notifStack ] ──
            LinearLayout leftRoot = new LinearLayout(context);
            leftRoot.setTag("dualline_left_root");
            leftRoot.setOrientation(LinearLayout.HORIZONTAL);
            leftRoot.setClipChildren(false);
            leftRoot.setClipToPadding(false);

            FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            rootLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            leftRoot.setLayoutParams(rootLp);

            // ── 1. leftStack: Kolom Jam & Tanggal (Tersendiri & 100% Stabil) ──
            LinearLayout leftStack = new LinearLayout(context);
            leftStack.setTag("dualline_left_stack");
            leftStack.setOrientation(LinearLayout.VERTICAL);
            leftStack.setClipChildren(false);
            leftStack.setClipToPadding(false);

            LinearLayout.LayoutParams stackLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            stackLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            stackLp.setMarginEnd((int) (4 * density)); // Jarak sebelum ikon notifikasi
            leftStack.setLayoutParams(stackLp);

            // ── BARIS 1 (weight=1): Clock — jam saja ──────────────────────
            LinearLayout row1 = new LinearLayout(context);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER);
            row1.setClipChildren(false);
            row1.setClipToPadding(false);
            LinearLayout.LayoutParams row1Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
            );
            row1Lp.gravity = Gravity.CENTER;
            row1.setLayoutParams(row1Lp);

            LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            clockLp.gravity = Gravity.CENTER;
            clockView.setLayoutParams(clockLp);
            clockView.setTranslationY(1.5f * density);
            row1.addView(clockView);

            // ── BARIS 2 (weight=1): DateView — tanggal (posisi KETENGAH) ─
            LinearLayout row2 = new LinearLayout(context);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setGravity(Gravity.CENTER);
            row2.setClipChildren(false);
            row2.setClipToPadding(false);
            LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
            );
            row2Lp.gravity = Gravity.CENTER;
            row2.setLayoutParams(row2Lp);

            DateView dateView = new DateView(context);
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dateLp.gravity = Gravity.CENTER;
            dateView.setLayoutParams(dateLp);
            dateView.setTranslationY(1.5f * density);
            row2.addView(dateView);

            leftStack.addView(row1);
            leftStack.addView(row2);
            leftRoot.addView(leftStack);

            // ── 2. secondStack: Kolom Kanan Sisi Kiri ─────────────────────
            // Baris 1: Suhu CPU & Baterai (di sebelah kanan jam)
            // Baris 2: Ikon Notifikasi (di sebelah kanan tanggal)
            LinearLayout secondStack = new LinearLayout(context);
            secondStack.setOrientation(LinearLayout.VERTICAL);
            secondStack.setClipChildren(false);
            secondStack.setClipToPadding(false);

            LinearLayout.LayoutParams secondStackLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            secondStackLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            secondStack.setLayoutParams(secondStackLp);

            // Baris 1: Suhu CPU & Baterai (Line 1 di samping kanan jam)
            LinearLayout thermalRow1 = new LinearLayout(context);
            thermalRow1.setOrientation(LinearLayout.HORIZONTAL);
            thermalRow1.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            thermalRow1.setClipChildren(false);
            thermalRow1.setClipToPadding(false);
            LinearLayout.LayoutParams thermalRow1Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1.0f
            );
            thermalRow1Lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            thermalRow1.setLayoutParams(thermalRow1Lp);

            ThermalView thermalView = new ThermalView(context);
            thermalView.setTag("dualline_thermal_view");
            thermalView.setTranslationY(1.5f * density);
            LinearLayout.LayoutParams thermalLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            thermalLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            thermalView.setLayoutParams(thermalLp);
            thermalRow1.addView(thermalView);

            // Baris 1: Info RAM (di samping kanan suhu)
            if (ModulePrefs.getBoolean(ModulePrefs.KEY_MEM_SHOW, true)) {
                MemView memView = new MemView(context);
                memView.setTag("dualline_mem_view");
                memView.setTranslationY(1.5f * density);
                LinearLayout.LayoutParams memLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                memLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                memView.setLayoutParams(memLp);
                thermalRow1.addView(memView);
                XposedBridge.log(TAG + ": Info RAM ditambahkan ke Line 1.");
            }

            secondStack.addView(thermalRow1);

            // Baris 2: Ikon Notifikasi (Line 2 di samping kanan tanggal)
            boolean showNotif = ModulePrefs.getBoolean(ModulePrefs.KEY_NOTIF_SHOW, true);
            if (notifContainer != null && showNotif) {
                LinearLayout notifRow2 = new LinearLayout(context);
                notifRow2.setOrientation(LinearLayout.HORIZONTAL);
                notifRow2.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                notifRow2.setClipChildren(false);
                notifRow2.setClipToPadding(false);
                LinearLayout.LayoutParams notifRow2Lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1.0f
                );
                notifRow2Lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                notifRow2.setLayoutParams(notifRow2Lp);

                float notifScale = ModulePrefs.getFloat(ModulePrefs.KEY_NOTIF_SCALE, 0.70f);
                notifContainer.setScaleX(notifScale);
                notifContainer.setScaleY(notifScale);
                notifContainer.setPivotX(0f);
                notifContainer.setPivotY(0.5f);
                notifContainer.setTranslationY(1.5f * density);

                if (notifContainer instanceof ViewGroup) {
                    ViewGroup nvg = (ViewGroup) notifContainer;
                    nvg.setClipChildren(false);
                    nvg.setClipToPadding(false);
                }

                LinearLayout.LayoutParams notifLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                notifLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                notifContainer.setLayoutParams(notifLp);

                notifRow2.addView(notifContainer);
                secondStack.addView(notifRow2);
            } else {
                View emptyBottom = new View(context);
                LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1.0f
                );
                emptyBottom.setLayoutParams(emptyLp);
                secondStack.addView(emptyBottom);
            }

            leftRoot.addView(secondStack);

            // Pasang leftRoot ke frameParent
            frameParent.addView(leftRoot);

            XposedBridge.log(TAG + ": BERHASIL susun kiri — Line 1: Jam | Line 2: Tanggal + Notifikasi!");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error susunSisiKiriDuaBaris: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    /**
     * Menyusun sisi kanan menjadi 2 baris:
     * - Baris 1: Sinyal -> Wi-Fi (StatusIconContainer)
     * - Baris 2: Network Traffic (Kiri) -> Baterai (Kanan)
     */
    private void susunSisiKananDuaBaris(View statusBarView, Context context) {
        try {
            if (statusBarView.findViewWithTag("dualline_right_stack") != null) {
                return;
            }

            // 1. Cari StatusIconContainer (Sinyal & Wi-Fi)
            View statusIcons = findViewByFilter(statusBarView, v -> {
                String cls = v.getClass().getSimpleName();
                String res = getResourceEntryName(v);
                return cls.contains("StatusIconContainer") || res.contains("statusicons");
            });

            // 2. Cari BatteryView (ComposeView / BatteryMeterView)
            View batteryView = findViewByFilter(statusBarView, v -> {
                String res = getResourceEntryName(v);
                String cls = v.getClass().getSimpleName();
                if (res.contains("batterybar") || res.contains("battery_bar")) return false;
                return res.contains("battery") || cls.contains("Battery");
            });

            // 3. Cari NetworkTrafficView bawaan ROM
            View netTrafficView = findViewByFilter(statusBarView, v -> {
                String cls = v.getClass().getSimpleName();
                String res = getResourceEntryName(v);
                return cls.contains("NetworkTraffic") || res.contains("network_traffic");
            });

            // 4. Cari kontainer sisi kanan (status_bar_end_side_content atau system_icons)
            View endSideContent = findViewByFilter(statusBarView, v -> {
                String res = getResourceEntryName(v);
                return res.contains("status_bar_end_side_content") || res.contains("system_icons");
            });

            if (endSideContent == null && batteryView != null && batteryView.getParent() instanceof ViewGroup) {
                endSideContent = (View) batteryView.getParent();
            }

            if (!(endSideContent instanceof ViewGroup)) {
                XposedBridge.log(TAG + ": Container sisi kanan tidak ditemukan.");
                return;
            }

            ViewGroup parentContainer = (ViewGroup) endSideContent;
            parentContainer.setClipChildren(false);
            parentContainer.setClipToPadding(false);

            // Buat container 2 baris vertikal dengan tinggi penuh (MATCH_PARENT)
            LinearLayout rightStack = new LinearLayout(context);
            rightStack.setTag("dualline_right_stack");
            rightStack.setOrientation(LinearLayout.VERTICAL);
            rightStack.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.END);
            rightStack.setClipChildren(false);
            rightStack.setClipToPadding(false);

            float density = context.getResources().getDisplayMetrics().density;
            int marginPx = (int) (3 * density);

            LinearLayout.LayoutParams stackLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            stackLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            rightStack.setLayoutParams(stackLp);

            // === BARIS 1 (ATAS): Sinyal & Wi-Fi (Kanan) ===
            LinearLayout row1 = new LinearLayout(context);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            row1.setClipChildren(false);
            row1.setClipToPadding(false);

            LinearLayout.LayoutParams row1Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                1.0f
            );
            row1Lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            row1.setLayoutParams(row1Lp);

            // Sinyal & Wi-Fi (StatusIconContainer)
            if (statusIcons != null) {
                if (statusIcons.getParent() instanceof ViewGroup) {
                    ((ViewGroup) statusIcons.getParent()).removeView(statusIcons);
                }
                float statusScale = ModulePrefs.getFloat(ModulePrefs.KEY_STATUS_ICONS_SCALE, 0.70f);
                statusIcons.setScaleX(statusScale);
                statusIcons.setScaleY(statusScale);
                LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                statusLp.gravity = Gravity.CENTER_VERTICAL;
                statusIcons.setLayoutParams(statusLp);
                row1.addView(statusIcons);
            }

            // === BARIS 2 (BAWAH): Network Traffic (Kiri) -> Baterai (Kanan) ===
            LinearLayout row2 = new LinearLayout(context);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            row2.setClipChildren(false);
            row2.setClipToPadding(false);

            LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                1.0f
            );
            row2Lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            row2.setLayoutParams(row2Lp);

            // 1. Network Traffic di sebelah KIRI baris 2 (ditaruh pertama)
            boolean showNetTraffic = ModulePrefs.getBoolean(ModulePrefs.KEY_NET_TRAFFIC_SHOW, true);
            if (netTrafficView != null && showNetTraffic) {
                if (netTrafficView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) netTrafficView.getParent()).removeView(netTrafficView);
                }

                netTrafficView.setScaleX(1.0f);
                netTrafficView.setScaleY(1.0f);
                // Geser ke bawah sedikit (1.5dp) agar sejajar visual atas-bawah dengan baterai
                netTrafficView.setTranslationY(1.5f * density);

                if (netTrafficView instanceof TextView) {
                    NetworkTrafficHook.configureNetworkTraffic((TextView) netTrafficView);
                } else if (netTrafficView instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) netTrafficView;
                    vg.setClipChildren(false);
                    vg.setClipToPadding(false);
                    vg.setPadding(0, 0, 0, 0);

                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View child = vg.getChildAt(i);
                        if (child instanceof TextView) {
                            NetworkTrafficHook.configureNetworkTraffic((TextView) child);
                        }
                    }
                }

                LinearLayout.LayoutParams netLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                netLp.gravity = Gravity.CENTER_VERTICAL;
                netLp.setMarginEnd(marginPx); // Jarak sebelum baterai
                netTrafficView.setLayoutParams(netLp);
                row2.addView(netTrafficView);
            }

            // 2. Baterai di sebelah KANAN baris 2 (ditaruh kedua / paling kanan)
            if (batteryView != null) {
                if (batteryView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) batteryView.getParent()).removeView(batteryView);
                }
                float batScale = ModulePrefs.getFloat(ModulePrefs.KEY_BATTERY_SCALE, 0.70f);
                batteryView.setScaleX(batScale);
                batteryView.setScaleY(batScale);
                LinearLayout.LayoutParams batLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                batLp.gravity = Gravity.CENTER_VERTICAL;
                batteryView.setLayoutParams(batLp);
                row2.addView(batteryView);
            }

            // Susun vertikal: Row 1 di atas, Row 2 di bawah
            rightStack.addView(row1);
            rightStack.addView(row2);

            // Pasang ke kontainer sisi kanan status bar
            parentContainer.addView(rightStack);
            XposedBridge.log(TAG + ": BERHASIL menyusun 2 baris kanan (Row 1: Sinyal+Wifi, Row 2: NetTraffic asli diperkecil + Baterai)!");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error saat susunSisiKananDuaBaris: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    private interface ViewPredicate {
        boolean test(View v);
    }

    private View findViewByFilter(View root, ViewPredicate predicate) {
        if (predicate.test(root)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findViewByFilter(vg.getChildAt(i), predicate);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String getResourceEntryName(View v) {
        try {
            int id = v.getId();
            if (id != View.NO_ID && id > 0) {
                return v.getResources().getResourceEntryName(id).toLowerCase();
            }
        } catch (Throwable ignored) {}
        return "";
    }

}
