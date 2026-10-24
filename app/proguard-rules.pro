# proguard-rules.pro - Aturan ProGuard untuk DualLineStatusBar
#
# PENTING: Jangan obfuscate kelas hook Xposed!
# LSPosed/Xposed memanggil kelas hook berdasarkan nama yang ada di xposed_init.
# Jika nama kelas berubah karena obfuscation, modul tidak akan bisa dimuat.

# Jaga semua kelas hook Xposed
-keep class com.dualline.statusbar.hooks.** { *; }

# Jaga SettingsActivity dan semua kelas UI
-keep class com.dualline.statusbar.ui.** { *; }

# Jaga Xposed API classes (compileOnly, tidak perlu bundle tapi tetap perlu jaga referensi)
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Jaga AndroidX Preference (dibutuhkan untuk SettingsActivity)
-keep class androidx.preference.** { *; }

# Jangan obfuscate nama method dan field (penting untuk refleksi Xposed)
-keepclassmembernames class * {
    public *;
    protected *;
}

# Log untuk debugging (biarkan StrackTrace tetap readable)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
