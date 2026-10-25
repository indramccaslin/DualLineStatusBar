package com.dualline.statusbar.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.dualline.statusbar.ModulePrefs;
import com.dualline.statusbar.R;

import java.util.ArrayList;
import java.util.List;

/**
 * SettingsActivity.java - UI Pengaturan Lengkap DualLineStatusBar
 */
public class SettingsActivity extends Activity {

    private SharedPreferences prefs;

    // Kontrol UI
    private Switch switchDualLine;
    private Switch switchClockSeconds;
    private Switch switchClockBold;
    private Spinner spinnerClockFormat;
    private Spinner spinnerClockSize;

    private Switch switchDateShow;
    private Switch switchDateBoldDay;
    private Spinner spinnerDateFormat;
    private Spinner spinnerDateAlignment;
    private Spinner spinnerDateSize;

    private Switch switchNotifShow;
    private Spinner spinnerNotifScale;

    private Switch switchThermalShow;
    private Switch switchMemShow;
    private Switch switchStorageShow;
    private Spinner spinnerThermalFormat;
    private Spinner spinnerThermalSpacing;
    private Spinner spinnerThermalInterval;
    private Spinner spinnerThermalSize;

    private Switch switchNetTrafficShow;
    private Spinner spinnerBatteryScale;
    private Spinner spinnerStatusScale;

    private Button btnRestartSystemUI;
    private Button btnResetDefaults;

    private boolean isBinding = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(ModulePrefs.PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        loadPreferences();
        setupListeners();

        // Pastikan izin file sudah benar
        ModulePrefs.fixPermissions(this);
    }

    private void initViews() {
        btnRestartSystemUI = findViewById(R.id.btn_restart_systemui);
        btnResetDefaults = findViewById(R.id.btn_reset_defaults);

        switchDualLine = findViewById(R.id.switch_enable_dual_line);
        switchClockSeconds = findViewById(R.id.switch_clock_seconds);
        switchClockBold = findViewById(R.id.switch_clock_bold);
        spinnerClockFormat = findViewById(R.id.spinner_clock_format);
        spinnerClockSize = findViewById(R.id.spinner_clock_size);

        switchDateShow = findViewById(R.id.switch_date_show);
        switchDateBoldDay = findViewById(R.id.switch_date_bold_day);
        spinnerDateFormat = findViewById(R.id.spinner_date_format);
        spinnerDateAlignment = findViewById(R.id.spinner_date_alignment);
        spinnerDateSize = findViewById(R.id.spinner_date_size);

        switchNotifShow = findViewById(R.id.switch_notif_show);
        spinnerNotifScale = findViewById(R.id.spinner_notif_scale);

        switchThermalShow = findViewById(R.id.switch_thermal_show);
        switchMemShow = findViewById(R.id.switch_mem_show);
        switchStorageShow = findViewById(R.id.switch_storage_show);
        spinnerThermalFormat = findViewById(R.id.spinner_thermal_format);
        spinnerThermalSpacing = findViewById(R.id.spinner_thermal_spacing);
        spinnerThermalInterval = findViewById(R.id.spinner_thermal_interval);
        spinnerThermalSize = findViewById(R.id.spinner_thermal_size);

        switchNetTrafficShow = findViewById(R.id.switch_net_traffic_show);
        spinnerBatteryScale = findViewById(R.id.spinner_battery_scale);
        spinnerStatusScale = findViewById(R.id.spinner_status_scale);
    }

    private void loadPreferences() {
        isBinding = true;

        // Master
        switchDualLine.setChecked(prefs.getBoolean(ModulePrefs.KEY_DUAL_LINE_ENABLED, true));

        // Jam
        switchClockSeconds.setChecked(prefs.getBoolean(ModulePrefs.KEY_CLOCK_SHOW_SECONDS, true));
        switchClockBold.setChecked(prefs.getBoolean(ModulePrefs.KEY_CLOCK_BOLD, true));
        setupSpinner(spinnerClockFormat,
            new String[]{"24 Jam dengan Detik (HH:mm:ss)", "24 Jam tanpa Detik (HH:mm)", "12 Jam dengan Detik (hh:mm:ss a)", "12 Jam tanpa Detik (hh:mm a)"},
            new String[]{"HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a"},
            prefs.getString(ModulePrefs.KEY_CLOCK_TIME_FORMAT, "HH:mm:ss")
        );
        setupSpinner(spinnerClockSize,
            new String[]{"Kecil (0.65x)", "Normal (0.75x)", "Besar (0.85x)"},
            new Float[]{0.65f, 0.75f, 0.85f},
            prefs.getFloat(ModulePrefs.KEY_CLOCK_SIZE_FACTOR, 0.75f)
        );

        // Tanggal
        switchDateShow.setChecked(prefs.getBoolean(ModulePrefs.KEY_DATE_SHOW, true));
        switchDateBoldDay.setChecked(prefs.getBoolean(ModulePrefs.KEY_DATE_BOLD_DAY, true));
        setupSpinner(spinnerDateFormat,
            new String[]{"Sab, 24 Okt (EEE, dd MMM)", "Sabtu, 24 Okt (EEEE, dd MMM)", "24 Okt 2026 (dd MMM yyyy)", "Sab, 24/10 (EEE, dd/MM)", "24/10/2026 (dd/MM/yyyy)"},
            new String[]{"EEE, dd MMM", "EEEE, dd MMM", "dd MMM yyyy", "EEE, dd/MM", "dd/MM/yyyy"},
            prefs.getString(ModulePrefs.KEY_DATE_FORMAT, "EEE, dd MMM")
        );
        setupSpinner(spinnerDateAlignment,
            new String[]{"Ketengah (Center)", "Rata Kiri (Start)"},
            new String[]{"center", "start"},
            prefs.getString(ModulePrefs.KEY_DATE_ALIGNMENT, "center")
        );
        setupSpinner(spinnerDateSize,
            new String[]{"Kecil (5.2dp)", "Normal (6.2dp)", "Besar (7.2dp)"},
            new Float[]{5.2f, 6.2f, 7.2f},
            prefs.getFloat(ModulePrefs.KEY_DATE_SIZE_DP, 6.2f)
        );

        // Notifikasi
        switchNotifShow.setChecked(prefs.getBoolean(ModulePrefs.KEY_NOTIF_SHOW, true));
        setupSpinner(spinnerNotifScale,
            new String[]{"Kecil (60%)", "Sedang (70%)", "Normal (80%)"},
            new Float[]{0.60f, 0.70f, 0.80f},
            prefs.getFloat(ModulePrefs.KEY_NOTIF_SCALE, 0.70f)
        );

        // Suhu
        switchThermalShow.setChecked(prefs.getBoolean(ModulePrefs.KEY_THERMAL_SHOW, true));
        setupSpinner(spinnerThermalFormat,
            new String[]{"1 Baris Memanjang (CPU 50°  BAT 50°)", "2 Baris Bertumpuk (CPU atas, BAT bawah)", "Hanya CPU (CPU 50°)", "Hanya Baterai (BAT 50°)"},
            new String[]{"single_line", "two_lines", "cpu_only", "bat_only"},
            prefs.getString(ModulePrefs.KEY_THERMAL_FORMAT, "single_line")
        );
        setupSpinner(spinnerThermalSpacing,
            new String[]{"1 Spasi", "2 Spasi (Standar)", "3 Spasi"},
            new Integer[]{1, 2, 3},
            prefs.getInt(ModulePrefs.KEY_THERMAL_SPACING, 2)
        );
        setupSpinner(spinnerThermalInterval,
            new String[]{"1 Detik (Sangat Cepat)", "2 Detik (Rekomendasi)", "5 Detik (Hemat Daya)"},
            new Integer[]{1, 2, 5},
            prefs.getInt(ModulePrefs.KEY_THERMAL_INTERVAL, 2)
        );
        setupSpinner(spinnerThermalSize,
            new String[]{"Kecil (5.0dp)", "Normal (5.8dp)", "Besar (6.5dp)"},
            new Float[]{5.0f, 5.8f, 6.5f},
            prefs.getFloat(ModulePrefs.KEY_THERMAL_SIZE_DP, 5.8f)
        );
        switchMemShow.setChecked(prefs.getBoolean(ModulePrefs.KEY_MEM_SHOW, true));
        switchStorageShow.setChecked(prefs.getBoolean(ModulePrefs.KEY_STORAGE_SHOW, true));

        // Network Traffic & Icons
        switchNetTrafficShow.setChecked(prefs.getBoolean(ModulePrefs.KEY_NET_TRAFFIC_SHOW, true));
        setupSpinner(spinnerBatteryScale,
            new String[]{"Kecil (70%)", "Sedang (80%)", "Normal (100%)"},
            new Float[]{0.70f, 0.80f, 1.0f},
            prefs.getFloat(ModulePrefs.KEY_BATTERY_SCALE, 0.70f)
        );
        setupSpinner(spinnerStatusScale,
            new String[]{"Kecil (70%)", "Sedang (80%)", "Normal (100%)"},
            new Float[]{0.70f, 0.80f, 1.0f},
            prefs.getFloat(ModulePrefs.KEY_STATUS_ICONS_SCALE, 0.70f)
        );

        isBinding = false;
    }

    private void setupListeners() {
        // Master switch
        switchDualLine.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_DUAL_LINE_ENABLED, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Jam
        switchClockSeconds.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_CLOCK_SHOW_SECONDS, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });
        switchClockBold.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_CLOCK_BOLD, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Tanggal
        switchDateShow.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_DATE_SHOW, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });
        switchDateBoldDay.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_DATE_BOLD_DAY, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Notifikasi
        switchNotifShow.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_NOTIF_SHOW, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Suhu
        switchThermalShow.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_THERMAL_SHOW, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Info RAM & Storage
        switchMemShow.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_MEM_SHOW, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });
        switchStorageShow.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_STORAGE_SHOW, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Network Traffic
        switchNetTrafficShow.setOnCheckedChangeListener((v, isChecked) -> {
            if (isBinding) return;
            prefs.edit().putBoolean(ModulePrefs.KEY_NET_TRAFFIC_SHOW, isChecked).commit();
            ModulePrefs.fixPermissions(this);
        });

        // Tombol Restart SystemUI
        btnRestartSystemUI.setOnClickListener(v -> restartSystemUI());

        // Tombol Reset Defaults
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
    }

    private void restartSystemUI() {
        Toast.makeText(this, "Menerapkan konfigurasi & me-restart SystemUI...", Toast.LENGTH_SHORT).show();
        ModulePrefs.fixPermissions(this);

        new Thread(() -> {
            try {
                Thread.sleep(300);
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "pkill -f com.android.systemui"});
                p.waitFor();
            } catch (Throwable t) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Gagal restart otomatis: " + t.getMessage() + ". Silakan restart via LSPosed atau reboot.", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void resetToDefaults() {
        prefs.edit().clear().commit();
        ModulePrefs.fixPermissions(this);
        loadPreferences();
        Toast.makeText(this, "Pengaturan dikembalikan ke konfigurasi standar.", Toast.LENGTH_SHORT).show();
    }

    // Helper Spinner String
    private void setupSpinner(Spinner spinner, String[] labels, String[] values, String currentValue) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentValue)) {
                selectedIndex = i;
                break;
            }
        }
        spinner.setSelection(selectedIndex);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isBinding) return;
                String selectedVal = values[pos];
                // Simpan ke prefs sesuai ID spinner
                if (spinner == spinnerClockFormat) {
                    prefs.edit().putString(ModulePrefs.KEY_CLOCK_TIME_FORMAT, selectedVal).commit();
                } else if (spinner == spinnerDateFormat) {
                    prefs.edit().putString(ModulePrefs.KEY_DATE_FORMAT, selectedVal).commit();
                } else if (spinner == spinnerDateAlignment) {
                    prefs.edit().putString(ModulePrefs.KEY_DATE_ALIGNMENT, selectedVal).commit();
                } else if (spinner == spinnerThermalFormat) {
                    prefs.edit().putString(ModulePrefs.KEY_THERMAL_FORMAT, selectedVal).commit();
                }
                ModulePrefs.fixPermissions(SettingsActivity.this);
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    // Helper Spinner Float
    private void setupSpinner(Spinner spinner, String[] labels, Float[] values, float currentValue) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - currentValue) < 0.01f) {
                selectedIndex = i;
                break;
            }
        }
        spinner.setSelection(selectedIndex);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isBinding) return;
                float selectedVal = values[pos];
                if (spinner == spinnerClockSize) {
                    prefs.edit().putFloat(ModulePrefs.KEY_CLOCK_SIZE_FACTOR, selectedVal).commit();
                } else if (spinner == spinnerDateSize) {
                    prefs.edit().putFloat(ModulePrefs.KEY_DATE_SIZE_DP, selectedVal).commit();
                } else if (spinner == spinnerNotifScale) {
                    prefs.edit().putFloat(ModulePrefs.KEY_NOTIF_SCALE, selectedVal).commit();
                } else if (spinner == spinnerThermalSize) {
                    prefs.edit().putFloat(ModulePrefs.KEY_THERMAL_SIZE_DP, selectedVal).commit();
                } else if (spinner == spinnerBatteryScale) {
                    prefs.edit().putFloat(ModulePrefs.KEY_BATTERY_SCALE, selectedVal).commit();
                } else if (spinner == spinnerStatusScale) {
                    prefs.edit().putFloat(ModulePrefs.KEY_STATUS_ICONS_SCALE, selectedVal).commit();
                }
                ModulePrefs.fixPermissions(SettingsActivity.this);
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    // Helper Spinner Integer
    private void setupSpinner(Spinner spinner, String[] labels, Integer[] values, int currentValue) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) {
                selectedIndex = i;
                break;
            }
        }
        spinner.setSelection(selectedIndex);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isBinding) return;
                int selectedVal = values[pos];
                if (spinner == spinnerThermalSpacing) {
                    prefs.edit().putInt(ModulePrefs.KEY_THERMAL_SPACING, selectedVal).commit();
                } else if (spinner == spinnerThermalInterval) {
                    prefs.edit().putInt(ModulePrefs.KEY_THERMAL_INTERVAL, selectedVal).commit();
                }
                ModulePrefs.fixPermissions(SettingsActivity.this);
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
}
