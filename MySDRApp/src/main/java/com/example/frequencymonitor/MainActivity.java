package com.example.frequencymonitor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "Frequenzy_Monitor_V26";
    private static final int PERMISSION_REQUEST_CODE = 102;
    private static final String PREFS_NAME = "FrequencyMonitorPrefs";

    // Default values
    private static final long DEFAULT_START_FREQ = 380_000_000;  // 380 MHz
    private static final long DEFAULT_STOP_FREQ = 385_000_000;   // 385 MHz
    private static final float DEFAULT_ALARM_THRESHOLD = -50.0f; // dBm
    private static final int SCAN_STEP_KHZ = 100;               // 100 KHz steps
    private static final int SAMPLES_PER_FREQ = 10;             // 10 samples per frequency
    private static final int SCAN_TIMEOUT_MS = 900;             // <1 second total scan

    // UI Elements
    private TextView statusText;
    private TextView frequencyText;
    private TextView signalText;
    private TextView peakFrequencyText;
    private ProgressBar progressBar;
    private ProgressBar signalBar;

    private EditText startFreqEdit;
    private EditText stopFreqEdit;
    private EditText thresholdEdit;
    private EditText stepEdit;

    private Button scanButton;
    private Button stopButton;
    private Button saveButton;
    private Button deviceButton;
    private Button presetButton;
    private Button settingsButton;
    private Button aboutButton;
    private ToggleButton muteButton;
    private ToggleButton alarmToggle;

    // State variables
    private boolean isScanning = false;
    private boolean isMuted = false;
    private boolean alarmEnabled = true;
    private long startFrequency = DEFAULT_START_FREQ;
    private long stopFrequency = DEFAULT_STOP_FREQ;
    private float alarmThreshold = DEFAULT_ALARM_THRESHOLD;
    private int scanStepKHz = SCAN_STEP_KHZ;

    // Peak tracking
    private float peakSignal = -100.0f;
    private long peakFrequency = startFrequency;
    private final List<Float> signalHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    // Audio
    private ToneGenerator toneGenerator;
    private Handler handler;
    private AudioManager audioManager;
    private int originalVolume;

    // SDR
    private RTLSDRDevice rtlsdrDevice;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initializeUI();
        loadPreferences();
        checkPermissions();
        initializeAudio();
        connectToUSB();
        updateFrequencyDisplay();
    }

    private void initializeUI() {
        statusText = findViewById(R.id.statusText);
        frequencyText = findViewById(R.id.frequencyText);
        signalText = findViewById(R.id.signalText);
        peakFrequencyText = findViewById(R.id.peakFrequencyText);
        progressBar = findViewById(R.id.progressBar);
        signalBar = findViewById(R.id.signalBar);

        startFreqEdit = findViewById(R.id.startFreqEdit);
        stopFreqEdit = findViewById(R.id.stopFreqEdit);
        thresholdEdit = findViewById(R.id.thresholdEdit);
        stepEdit = findViewById(R.id.stepEdit);

        scanButton = findViewById(R.id.scanButton);
        stopButton = findViewById(R.id.stopButton);
        saveButton = findViewById(R.id.saveButton);
        muteButton = findViewById(R.id.muteButton);
        alarmToggle = findViewById(R.id.alarmToggle);
        deviceButton = findViewById(R.id.deviceButton);
        presetButton = findViewById(R.id.presetButton);
        settingsButton = findViewById(R.id.settingsButton);
        aboutButton = findViewById(R.id.aboutButton);

        scanButton.setOnClickListener(v -> startFrequencyScan());
        stopButton.setOnClickListener(v -> stopFrequencyScan());
        saveButton.setOnClickListener(v -> saveSettings());
        muteButton.setOnCheckedChangeListener((buttonView, isChecked) -> toggleMute(isChecked));
        alarmToggle.setOnCheckedChangeListener((buttonView, isChecked) -> alarmEnabled = isChecked);

        deviceButton.setOnClickListener(v -> showToast("Gerätesteuerung: USB- und SD-Status werden hier überwacht."));
        presetButton.setOnClickListener(v -> applyPreset("FM 88-108 MHz"));
        settingsButton.setOnClickListener(v -> showToast("Einstellungen: mehr Optionen werden hier ergänzt."));
        aboutButton.setOnClickListener(v -> showToast("Frequenzy Monitor: experimenteller SDR-Builder"));

        updateUIValues();
        updateUI();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        startFrequency = prefs.getLong("startFreq", DEFAULT_START_FREQ);
        stopFrequency = prefs.getLong("stopFreq", DEFAULT_STOP_FREQ);
        alarmThreshold = prefs.getFloat("threshold", DEFAULT_ALARM_THRESHOLD);
        scanStepKHz = prefs.getInt("step", SCAN_STEP_KHZ);
    }

    private void updateUIValues() {
        startFreqEdit.setText(String.valueOf(startFrequency / 1_000_000));
        stopFreqEdit.setText(String.valueOf(stopFrequency / 1_000_000));
        thresholdEdit.setText(String.format("%.1f", alarmThreshold));
        stepEdit.setText(String.valueOf(scanStepKHz));
    }

    private void saveSettings() {
        try {
            startFrequency = Long.parseLong(startFreqEdit.getText().toString()) * 1_000_000;
            stopFrequency = Long.parseLong(stopFreqEdit.getText().toString()) * 1_000_000;
            alarmThreshold = Float.parseFloat(thresholdEdit.getText().toString());
            scanStepKHz = Integer.parseInt(stepEdit.getText().toString());

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putLong("startFreq", startFrequency);
            editor.putLong("stopFreq", stopFrequency);
            editor.putFloat("threshold", alarmThreshold);
            editor.putInt("step", scanStepKHz);
            editor.apply();

            Toast.makeText(this, "✅ Einstellungen gespeichert", Toast.LENGTH_SHORT).show();
            updateFrequencyDisplay();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ Ungültige Eingabe", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void initializeAudio() {
        try {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception e) {
            Log.e(TAG, "Audio initialization failed", e);
        }
    }

    private void connectToUSB() {
        new Thread(() -> {
            try {
                UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                if (usbManager == null) {
                    runOnUiThread(() -> statusText.setText("❌ USB-Manager nicht verfügbar"));
                    return;
                }

                for (UsbDevice device : usbManager.getDeviceList().values()) {
                    if (isRTLSDR(device)) {
                        rtlsdrDevice = new RTLSDRDevice(device, usbManager, this);
                        if (rtlsdrDevice.connect()) {
                            isConnected = true;
                            runOnUiThread(() -> {
                                statusText.setText("✅ SDR erkannt");
                                frequencyText.setText(String.format("Gerät: %s\nBereich: %.0f - %.0f MHz", device.getDeviceName(), startFrequency / 1_000_000.0, stopFrequency / 1_000_000.0));
                            });
                            Log.d(TAG, "SDR device connected: " + device.getDeviceName());
                            return;
                        }
                    }
                }
                runOnUiThread(() -> statusText.setText("❌ Kein kompatibles SDR gefunden"));
            } catch (Exception e) {
                Log.e(TAG, "USB connection error", e);
                runOnUiThread(() -> statusText.setText("❌ USB-Verbindung fehlgeschlagen"));
            }
        }).start();
    }

    private boolean isRTLSDR(UsbDevice device) {
        return (device.getVendorId() == 0x0bda &&
                (device.getProductId() == 0x2832 || device.getProductId() == 0x2838 || device.getProductId() == 0x2880 || device.getProductId() == 0x2881 || device.getProductId() == 0x2882 || device.getProductId() == 0x2883));
    }

    private void startFrequencyScan() {
        if (!isConnected || rtlsdrDevice == null) {
            Toast.makeText(this, "❌ SDR nicht verbunden", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        peakSignal = -100.0f;
        peakFrequency = startFrequency;
        signalHistory.clear();
        updateUI();
        statusText.setText("🔍 Scan läuft...");
        progressBar.setProgress(0);

        new Thread(this::performFrequencyScan).start();
    }

    private void performFrequencyScan() {
        try {
            rtlsdrDevice.setSampleRate(2_400_000);
            rtlsdrDevice.setGain(450);

            long frequencyRange = stopFrequency - startFrequency;
            long frequencyStepHz = scanStepKHz * 1000L;
            int totalSteps = (int) (frequencyRange / frequencyStepHz) + 1;
            long timePerStep = Math.max(5, SCAN_TIMEOUT_MS / totalSteps);

            int currentStep = 0;
            for (long freq = startFrequency; freq <= stopFrequency && isScanning; freq += frequencyStepHz) {
                rtlsdrDevice.setCenterFrequency(freq);

                float maxSignal = -100.0f;
                for (int i = 0; i < SAMPLES_PER_FREQ && isScanning; i++) {
                    float signal = rtlsdrDevice.readSignalStrength();
                    if (signal > maxSignal) maxSignal = signal;
                    Thread.sleep(Math.max(1, timePerStep / SAMPLES_PER_FREQ));
                }

                if (maxSignal > peakSignal) {
                    peakSignal = maxSignal;
                    peakFrequency = freq;
                }

                signalHistory.add(maxSignal);
                if (signalHistory.size() > MAX_HISTORY) {
                    signalHistory.remove(0);
                }

                final int progress = (int) ((float) currentStep / totalSteps * 100);
                final float signal = maxSignal;
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    signalText.setText(String.format("Aktuell: %.1f dBm", signal));
                    updateSignalBar(signal);
                });

                if (maxSignal > alarmThreshold && alarmEnabled && !isMuted) {
                    playAlarm();
                    runOnUiThread(() -> statusText.setText(String.format("⚠️ Alarm: %.1f dBm @ %.2f MHz", maxSignal, freq / 1_000_000.0)));
                }

                currentStep++;
            }

            if (isScanning) {
                runOnUiThread(this::displayScanResults);
            }

        } catch (Exception e) {
            Log.e(TAG, "Scan error", e);
            runOnUiThread(() -> statusText.setText("❌ Scan-Fehler: " + e.getMessage()));
        } finally {
            isScanning = false;
            updateUI();
        }
    }

    private void displayScanResults() {
        String results = String.format(
                "📊 Scan abgeschlossen\nPeak: %.1f dBm @ %.2f MHz\nBereich: %.0f - %.0f MHz",
                peakSignal,
                peakFrequency / 1_000_000.0,
                startFrequency / 1_000_000.0,
                stopFrequency / 1_000_000.0
        );
        statusText.setText(results);
        peakFrequencyText.setText(String.format("Peak: %.2f MHz | %.1f dBm",
                peakFrequency / 1_000_000.0, peakSignal));
        progressBar.setProgress(100);
    }

    private void stopFrequencyScan() {
        isScanning = false;
        statusText.setText("⏹️ Scan gestoppt");
        updateUI();
    }

    private void playAlarm() {
        if (isMuted || toneGenerator == null) return;

        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        } catch (Exception e) {
            Log.e(TAG, "Alarm error", e);
        }
    }

    private void toggleMute(boolean muted) {
        isMuted = muted;
        if (muted) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
            muteButton.setText("🔇 Muted");
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
            muteButton.setText("🔊 Unmute");
        }
    }

    private void updateFrequencyDisplay() {
        frequencyText.setText(String.format(
                "Bereich: %.0f - %.0f MHz\nSchwelle: %.1f dBm",
                startFrequency / 1_000_000.0,
                stopFrequency / 1_000_000.0,
                alarmThreshold
        ));
    }

    private void updateSignalBar(float signal) {
        int barValue = (int) ((signal + 100) * 2.55);
        signalBar.setProgress(Math.max(0, Math.min(255, barValue)));
    }

    private void updateUI() {
        startFreqEdit.setEnabled(!isScanning);
        stopFreqEdit.setEnabled(!isScanning);
        thresholdEdit.setEnabled(!isScanning);
        stepEdit.setEnabled(!isScanning);
        saveButton.setEnabled(!isScanning);
        scanButton.setEnabled(!isScanning && isConnected);
        stopButton.setEnabled(isScanning);
    }

    private void applyPreset(String presetName) {
        if (presetName.equals("FM 88-108 MHz")) {
            startFrequency = 88_000_000L;
            stopFrequency = 108_000_000L;
            alarmThreshold = -55f;
            scanStepKHz = 100;
            updateUIValues();
            saveSettings();
            showToast("Preset FM 88-108 MHz aktiviert");
            updateFrequencyDisplay();
            return;
        }

        startFrequency = DEFAULT_START_FREQ;
        stopFrequency = DEFAULT_STOP_FREQ;
        alarmThreshold = DEFAULT_ALARM_THRESHOLD;
        scanStepKHz = SCAN_STEP_KHZ;
        updateUIValues();
        saveSettings();
        showToast("Preset zurückgesetzt");
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        if (rtlsdrDevice != null) {
            rtlsdrDevice.disconnect();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
