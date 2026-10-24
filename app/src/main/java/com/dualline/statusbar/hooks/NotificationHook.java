package com.dualline.statusbar.hooks;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * NotificationHook.java - Hook untuk area notifikasi di status bar
 *
 * Hook ini bertujuan untuk mengizinkan ikon notifikasi "wrap" ke baris kedua
 * status bar yang sudah kita buat dengan StatusBarHook.
 *
 * Area notifikasi di SystemUI dikelola oleh beberapa class:
 * - NotificationIconAreaController: mengatur area ikon notifikasi
 * - NotificationShelf: shelf untuk notifikasi yang tersembunyi
 * - StatusBarIconController: mengontrol semua ikon di status bar
 *
 * Strategi:
 * 1. Hook NotificationIconAreaController untuk mendapat akses ke container view
 * 2. Modifikasi container agar tidak memotong (clip) ikon yang meluber
 * 3. Jika overflow, ikon bisa mengisi baris kedua yang sudah disiapkan
 *
 * Catatan untuk AOSP17 / ProjectInfinity X 4.1:
 * Nama class mungkin berbeda, semua hook dibungkus try-catch untuk keamanan.
 *
 * @author DualLineStatusBar Module
 * @version 1.0.0
 */
public class NotificationHook {

    private static final String TAG = "DualLineStatusBar/Notification";

    // Nama-nama class yang berhubungan dengan area notifikasi
    // Diurutkan dari yang paling mungkin ada di AOSP 14/15
    private static final String NOTIF_ICON_AREA_CONTROLLER =
        "com.android.systemui.statusbar.phone.NotificationIconAreaController";
    private static final String NOTIF_ICON_AREA_CONTROLLER_IMPL =
        "com.android.systemui.statusbar.phone.NotificationIconAreaControllerImpl";
    private static final String STATUS_BAR_ICON_CONTROLLER =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl";
    private static final String NOTIF_ICON_CONTAINER =
        "com.android.systemui.statusbar.phone.NotificationIconContainer";

    // Kunci SharedPreferences
    private static final String PREFS_NAME = "dualline_prefs";
    private static final String PREF_DUAL_LINE_ENABLED = "pref_dual_line_enabled";

    /**
     * Menginisialisasi semua hooks terkait notifikasi.
     * Menggunakan pendekatan best-effort: jika satu hook gagal, lanjutkan ke berikutnya.
     *
     * @param lpparam Parameter LoadPackage dari Xposed
     */
    public void initHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        // Hook NotificationIconAreaController
        hookNotificationIconArea(lpparam);

        // Hook NotificationIconContainer langsung
        hookNotificationIconContainer(lpparam);

        // Hook StatusBarIconController untuk mengizinkan lebih banyak ikon
        hookStatusBarIconController(lpparam);

        XposedBridge.log(TAG + ": Inisialisasi NotificationHook selesai.");
    }

    private void hookNotificationIconContainer(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> containerClass = XposedHelpers.findClass(NOTIF_ICON_CONTAINER, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(
                containerClass,
                "onFinishInflate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup container = (ViewGroup) param.thisObject;
                        container.setClipChildren(false);
                        container.setClipToPadding(false);
                        XposedBridge.log(TAG + ": NotificationIconContainer onFinishInflate hooked (clip=false)");
                    }
                }
            );
            XposedBridge.log(TAG + ": Hook " + NOTIF_ICON_CONTAINER + ".onFinishInflate() berhasil dipasang.");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Tidak bisa hook NotificationIconContainer: " + t.getMessage());
        }
    }

    /**
     * Hook NotificationIconAreaController untuk mendapatkan akses ke container
     * area notifikasi dan memodifikasinya agar support wrap ke baris kedua.
     *
     * Method yang di-hook: onDensityOrFontScaleChanged() dan updateNotificationIcons()
     * Kedua method ini dipanggil saat layout perlu diperbarui.
     *
     * @param lpparam Parameter LoadPackage
     */
    private void hookNotificationIconArea(XC_LoadPackage.LoadPackageParam lpparam) {
        // Coba class utama terlebih dahulu
        String[] candidateClasses = {
            NOTIF_ICON_AREA_CONTROLLER,
            NOTIF_ICON_AREA_CONTROLLER_IMPL
        };

        for (String className : candidateClasses) {
            try {
                Class<?> controllerClass = XposedHelpers.findClass(className, lpparam.classLoader);

                // Hook method yang dipanggil saat ikon notifikasi perlu diperbarui
                // Ini adalah titik yang baik untuk memodifikasi container
                hookUpdateNotificationIcons(controllerClass);

                XposedBridge.log(TAG + ": Hook " + className + " berhasil dipasang.");
                return; // Berhasil, tidak perlu coba class berikutnya

            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log(TAG + ": Class " + className + " tidak ditemukan, mencoba berikutnya...");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Error saat hook " + className + ": " + t.getMessage());
                XposedBridge.log(t);
            }
        }

        XposedBridge.log(TAG + ": PERINGATAN - Tidak bisa hook NotificationIconAreaController manapun.");
    }

    /**
     * Hook method updateNotificationIcons() atau onDensityOrFontScaleChanged()
     * untuk mendapat akses ke container view notifikasi.
     *
     * @param controllerClass Class yang akan di-hook
     */
    private void hookUpdateNotificationIcons(Class<?> controllerClass) {
        // Nama-nama method yang mungkin ada (tergantung versi AOSP)
        String[] kandidatMetode = {
            "updateNotificationIcons",
            "onDensityOrFontScaleChanged",
            "updateIconsForLayout",
            "updateAodNotificationIcons",
            "initializeNotificationAreaViews"
        };

        boolean adaYangBerhasil = false;

        for (String namaMetode : kandidatMetode) {
            try {
                XposedHelpers.findAndHookMethod(
                    controllerClass,
                    namaMetode,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                // Coba dapatkan container view dari field
                                modifikasiContainerNotifikasi(param.thisObject);
                            } catch (Throwable t) {
                                // Tidak fatal - log dan lanjutkan
                                XposedBridge.log(TAG + ": Error di " + namaMetode + " hook: " + t.getMessage());
                            }
                        }
                    }
                );
                XposedBridge.log(TAG + ": Hook method " + namaMetode + "() berhasil.");
                adaYangBerhasil = true;
                // Tidak break - pasang hook ke semua method yang ditemukan
            } catch (NoSuchMethodError e) {
                // Method ini tidak ada di class ini, coba berikutnya
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Error hook method " + namaMetode + ": " + t.getMessage());
            }
        }

        if (!adaYangBerhasil) {
            XposedBridge.log(TAG + ": Tidak ada method updateNotificationIcons yang ditemukan.");
        }
    }

    /**
     * Memodifikasi container ViewGroup yang menampung ikon notifikasi.
     * Kita coba berbagai nama field untuk menemukan container yang tepat.
     *
     * @param controllerObject Instance dari controller class
     */
    private void modifikasiContainerNotifikasi(Object controllerObject) {
        // Nama-nama field yang mungkin menyimpan container view
        String[] candidateFieldNames = {
            "mNotificationIconArea",
            "mNotificationIcons",
            "mShelfState",
            "mIconsView",
            "mStatusIconsView",
            "notificationIconContainer",
            "mContainerView"
        };

        for (String fieldName : candidateFieldNames) {
            try {
                Object fieldValue = XposedHelpers.getObjectField(controllerObject, fieldName);

                if (fieldValue instanceof ViewGroup) {
                    ViewGroup container = (ViewGroup) fieldValue;

                    // Nonaktifkan clipping agar ikon bisa meluber ke baris kedua
                    container.setClipChildren(false);
                    container.setClipToPadding(false);

                    // Pastikan parent juga tidak clip
                    if (container.getParent() instanceof ViewGroup) {
                        ViewGroup parent = (ViewGroup) container.getParent();
                        parent.setClipChildren(false);
                        parent.setClipToPadding(false);
                    }

                    XposedBridge.log(TAG + ": Field " + fieldName + " berhasil dimodifikasi (clipChildren=false).");
                    return; // Berhasil, tidak perlu coba field lain
                }
            } catch (Throwable e) {
                // Field tidak ada atau bukan ViewGroup, coba berikutnya
            }
        }
    }

    /**
     * Hook StatusBarIconController untuk mengizinkan lebih banyak ikon ditampilkan.
     * Di AOSP, ada batas jumlah ikon yang ditampilkan. Hook ini meningkatkan batas tersebut
     * atau menonaktifkannya agar ikon bisa wrap ke baris kedua.
     *
     * @param lpparam Parameter LoadPackage
     */
    private void hookStatusBarIconController(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> iconControllerClass = XposedHelpers.findClass(
                STATUS_BAR_ICON_CONTROLLER,
                lpparam.classLoader
            );

            // Hook method yang menentukan apakah ikon harus disembunyikan
            // Nama method ini bervariasi antar ROM
            String[] kandidatMetode = {"shouldHideOverflowIcons", "isMaximumReached", "willIconBeHidden"};

            for (String namaMetode : kandidatMetode) {
                try {
                    XposedHelpers.findAndHookMethod(
                        iconControllerClass,
                        namaMetode,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                // Selalu kembalikan false = ikon tidak perlu disembunyikan
                                // Ini membuat semua ikon notifikasi tampil, bukan hanya sebagian
                                param.setResult(false);
                            }
                        }
                    );
                    XposedBridge.log(TAG + ": Hook " + namaMetode + "() berhasil - overflow icons diizinkan.");
                } catch (NoSuchMethodError e) {
                    // Method tidak ada di class ini, skip
                }
            }

            // Hook method getMaxIcons() jika ada, kembalikan nilai lebih besar
            try {
                XposedHelpers.findAndHookMethod(
                    iconControllerClass,
                    "getMaxIcons",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // Lipat gandakan jumlah ikon maksimum
                            Object result = param.getResult();
                            if (result instanceof Integer) {
                                param.setResult((Integer) result * 2);
                            }
                        }
                    }
                );
                XposedBridge.log(TAG + ": Hook getMaxIcons() berhasil - kapasitas ikon digandakan.");
            } catch (NoSuchMethodError e) {
                // getMaxIcons() tidak ada di class ini, tidak masalah
            }

        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(TAG + ": StatusBarIconControllerImpl tidak ditemukan: " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error saat hook StatusBarIconController: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    /**
     * Membaca pengaturan dual line dari SharedPreferences modul.
     *
     * @param context Context dari proses SystemUI
     * @return true jika dual line diaktifkan
     */
    private boolean isDualLineEnabled(Context context) {
        try {
            SharedPreferences prefs = context.createPackageContext(
                "com.dualline.statusbar",
                Context.CONTEXT_IGNORE_SECURITY
            ).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(PREF_DUAL_LINE_ENABLED, true);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Tidak bisa baca prefs: " + t.getMessage());
            return true; // Default aktif
        }
    }
}
