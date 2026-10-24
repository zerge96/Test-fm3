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
    private static final int MAX_SUPPORTED_DEVICES = 2;
    private static final int MAX_SCAN_BANDWIDTH_HZ = 2_500_000;

    public static final String MODULATION_FM = "FM";
    public static final String MODULATION_AM = "AM";
    public static final String MODULATION_TETRA = "TETRA";

    // Default values
    private static final long DEFAULT_START_FREQ = 380_000_000L; // 380 MHz
    private static final long DEFAULT_STOP_FREQ = 385_000_000L;  // 385 MHz
    private static final float DEFAULT_ALARM_THRESHOLD = -50.0f; // dBm
    private static final int SCAN_STEP_KHZ = 100; // 100 KHz steps
    private static final int SAMPLES_PER_FREQ = 10;
    private static final int SCAN_TIMEOUT_MS = 900;

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
    private Button modulationButton;
    private Button presetButton;
    private Button settingsButton;
    private Button aboutButton;
    private ToggleButton muteButton;
    private ToggleButton alarmToggle;

    private boolean isScanning = false;
    private boolean isMuted = false;
    private boolean alarmEnabled = true;
    private long startFrequency = DEFAULT_START_FREQ;
    private long stopFrequency = DEFAULT_STOP_FREQ;
    private float alarmThreshold = DEFAULT_ALARM_THRESHOLD;
    private int scanStepKHz = SCAN_STEP_KHZ;
    private String activeModulation = MODULATION_TETRA;

    private float peakSignal = -100.0f;
    private long peakFrequency = startFrequency;
    private final List<Float> signalHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    private ToneGenerator toneGenerator;
    private Handler handler;
    private AudioManager audioManager;
    private int originalVolume;

    private final List<RTLSDRDevice> connectedDevices = new ArrayList<>();
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
        deviceButton = findViewById(R.id.deviceButton);
        modulationButton = findViewById(R.id.modulationButton);
        presetButton = findViewById(R.id.presetButton);
        settingsButton = findViewById(R.id.settingsButton);
        aboutButton = findViewById(R.id.aboutButton);
        muteButton = findViewById(R.id.muteButton);
        alarmToggle = findViewById(R.id.alarmToggle);

        scanButton.setOnClickListener(v -> startFrequencyScan());
        stopButton.setOnClickListener(v -> stopFrequencyScan());
        saveButton.setOnClickListener(v -> saveSettings());
        muteButton.setOnCheckedChangeListener((buttonView, isChecked) -> toggleMute(isChecked));
        alarmToggle.setOnCheckedChangeListener((buttonView, isChecked) -> alarmEnabled = isChecked);

        deviceButton.setOnClickListener(v -> showDeviceStatus());
        modulationButton.setOnClickListener(v -> cycleModulation());
        presetButton.setOnClickListener(v -> applyPreset(activeModulation));
        settingsButton.setOnClickListener(v -> showToast("Erweiterte Einstellungen: Gain, Band und Modulation werden hier ergänzt."));
        aboutButton.setOnClickListener(v -> showToast("Frequenzy Monitor: SDR-Experiment mit TETRA, FM und AM"));

        updateUIValues();
        updateUI();
        updateModulationButtonText();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        startFrequency = prefs.getLong("startFreq", DEFAULT_START_FREQ);
        stopFrequency = prefs.getLong("stopFreq", DEFAULT_STOP_FREQ);
        alarmThreshold = prefs.getFloat("threshold", DEFAULT_ALARM_THRESHOLD);
        scanStepKHz = prefs.getInt("step", SCAN_STEP_KHZ);
        activeModulation = prefs.getString("modulation", MODULATION_TETRA);
        if (activeModulation == null || activeModulation.trim().isEmpty()) {
            activeModulation = MODULATION_TETRA;
        }
    }

    private void updateUIValues() {
        startFreqEdit.setText(String.valueOf(startFrequency / 1_000_000L));
        stopFreqEdit.setText(String.valueOf(stopFrequency / 1_000_000L));
        thresholdEdit.setText(String.format("%.1f", alarmThreshold));
        stepEdit.setText(String.valueOf(scanStepKHz));
    }

    private void saveSettings() {
        try {
            startFrequency = Long.parseLong(startFreqEdit.getText().toString()) * 1_000_000L;
            stopFrequency = Long.parseLong(stopFreqEdit.getText().toString()) * 1_000_000L;
            alarmThreshold = Float.parseFloat(thresholdEdit.getText().toString());
            scanStepKHz = Integer.parseInt(stepEdit.getText().toString());

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putLong("startFreq", startFrequency);
            editor.putLong("stopFreq", stopFrequency);
            editor.putFloat("threshold", alarmThreshold);
            editor.putInt("step", scanStepKHz);
            editor.putString("modulation", activeModulation);
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

                connectedDevices.clear();
                List<UsbDevice> candidates = new ArrayList<>();
                for (UsbDevice device : usbManager.getDeviceList().values()) {
                    if (RTLSDRDevice.isSupportedDevice(device)) {
                        candidates.add(device);
                    }
                }

                int count = 0;
                for (UsbDevice device : candidates) {
                    if (count >= MAX_SUPPORTED_DEVICES) {
                        break;
                    }

                    RTLSDRDevice rtlsdrDevice = new RTLSDRDevice(device, usbManager, this);
                    if (rtlsdrDevice.connect()) {
                        connectedDevices.add(rtlsdrDevice);
                        count++;
                    }
                }

                isConnected = !connectedDevices.isEmpty();
                if (isConnected) {
                    runOnUiThread(() -> {
                        statusText.setText("✅ " + connectedDevices.size() + " SDR-Gerät(e) erkannt");
                        frequencyText.setText(String.format("Modulation: %s\nGeräte: %d\nBereich: %.0f - %.0f MHz",
                                activeModulation,
                                connectedDevices.size(),
                                startFrequency / 1_000_000.0,
                                stopFrequency / 1_000_000.0));
                    });
                    Log.d(TAG, "Detected SDR device count: " + connectedDevices.size());
                    return;
                }

                runOnUiThread(() -> statusText.setText("❌ Kein kompatibles SDR gefunden"));
            } catch (Exception e) {
                Log.e(TAG, "USB connection error", e);
                runOnUiThread(() -> statusText.setText("❌ USB-Verbindung fehlgeschlagen"));
            }
        }).start();
    }

    private void showDeviceStatus() {
        if (connectedDevices.isEmpty()) {
            showToast("Keine kompatiblen Dongles erkannt.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < connectedDevices.size(); i++) {
            RTLSDRDevice device = connectedDevices.get(i);
            builder.append("Dongle ")
                    .append(i + 1)
                    .append(": ")
                    .append(device.getUsbDevice() != null ? device.getUsbDevice().getDeviceName() : "USB")
                    .append("\n");
        }
        builder.append("Max. Scan pro Dongle: 2.5 MHz");
        showToast(builder.toString());
    }

    private void cycleModulation() {
        if (MODULATION_TETRA.equals(activeModulation)) {
            activeModulation = MODULATION_FM;
        } else if (MODULATION_FM.equals(activeModulation)) {
            activeModulation = MODULATION_AM;
        } else {
            activeModulation = MODULATION_TETRA;
        }

        updateModulationButtonText();
        applyPreset(activeModulation);
        showToast("Modulation: " + activeModulation);
    }

    private void updateModulationButtonText() {
        if (modulationButton != null) {
            modulationButton.setText("Mod: " + activeModulation);
        }
    }

    private void startFrequencyScan() {
        if (connectedDevices.isEmpty()) {
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
            long totalRange = stopFrequency - startFrequency;
            int deviceCount = Math.max(1, Math.min(connectedDevices.size(), MAX_SUPPORTED_DEVICES));
            long segmentSize = Math.min(MAX_SCAN_BANDWIDTH_HZ, Math.max(1, totalRange / deviceCount));
            long currentStart = startFrequency;

            for (int deviceIndex = 0; deviceIndex < connectedDevices.size() && isScanning; deviceIndex++) {
                RTLSDRDevice device = connectedDevices.get(deviceIndex);
                long deviceStart = currentStart;
                long deviceStop = Math.min(deviceStart + segmentSize, stopFrequency);

                if (deviceStop <= deviceStart) {
                    deviceStop = stopFrequency;
                }

                device.setSampleRate(2_400_000);
                device.setGain(450);
                device.setCenterFrequency(deviceStart);

                long scanStop = Math.min(deviceStop, stopFrequency);
                for (long freq = deviceStart; freq <= scanStop && isScanning; freq += scanStepKHz * 1000L) {
                    device.setCenterFrequency(freq);
                    float maxSignal = -100.0f;
                    for (int i = 0; i < SAMPLES_PER_FREQ && isScanning; i++) {
                        float signal = device.readSignalStrength();
                        if (signal > maxSignal) maxSignal = signal;
                        Thread.sleep(Math.max(1, SCAN_TIMEOUT_MS / SAMPLES_PER_FREQ));
                    }

                    if (maxSignal > peakSignal) {
                        peakSignal = maxSignal;
                        peakFrequency = freq;
                    }

                    signalHistory.add(maxSignal);
                    if (signalHistory.size() > MAX_HISTORY) {
                        signalHistory.remove(0);
                    }

                    final int progress = (int) ((float) (freq - startFrequency) / totalRange * 100.0f);
                    final float signal = maxSignal;
                    runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                        signalText.setText(String.format("Aktuell: %.1f dBm (%s)", signal, activeModulation));
                        updateSignalBar(signal);
                    });

                    if (maxSignal > alarmThreshold && alarmEnabled && !isMuted) {
                        playAlarm();
                        runOnUiThread(() -> statusText.setText(String.format("⚠️ Alarm: %.1f dBm @ %.2f MHz [%s]",
                                maxSignal, freq / 1_000_000.0, activeModulation)));
                    }
                }

                currentStart = scanStop + 1;
                if (currentStart >= stopFrequency) {
                    break;
                }
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
                "📊 Scan abgeschlossen\nPeak: %.1f dBm @ %.2f MHz\nModulation: %s\nGeräte: %d",
                peakSignal,
                peakFrequency / 1_000_000.0,
                activeModulation,
                connectedDevices.size()
        );
        statusText.setText(results);
        peakFrequencyText.setText(String.format("Peak: %.2f MHz | %.1f dBm", peakFrequency / 1_000_000.0, peakSignal));
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
                "Modulation: %s\nBereich: %.0f - %.0f MHz\nSchwelle: %.1f dBm",
                activeModulation,
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
        if (MODULATION_TETRA.equalsIgnoreCase(presetName)) {
            startFrequency = 380_000_000L;
            stopFrequency = 385_000_000L;
            alarmThreshold = -45f;
            scanStepKHz = 25;
            activeModulation = MODULATION_TETRA;
            updateUIValues();
            saveSettings();
            showToast("Preset TETRA aktiviert");
            updateFrequencyDisplay();
            return;
        }

        if (MODULATION_FM.equalsIgnoreCase(presetName)) {
            startFrequency = 88_000_000L;
            stopFrequency = 108_000_000L;
            alarmThreshold = -55f;
            scanStepKHz = 100;
            activeModulation = MODULATION_FM;
            updateUIValues();
            saveSettings();
            showToast("Preset FM 88-108 MHz aktiviert");
            updateFrequencyDisplay();
            return;
        }

        if (MODULATION_AM.equalsIgnoreCase(presetName)) {
            startFrequency = 530_000L;
            stopFrequency = 1700_000L;
            alarmThreshold = -60f;
            scanStepKHz = 100;
            activeModulation = MODULATION_AM;
            updateUIValues();
            saveSettings();
            showToast("Preset AM aktiviert");
            updateFrequencyDisplay();
            return;
        }

        startFrequency = DEFAULT_START_FREQ;
        stopFrequency = DEFAULT_STOP_FREQ;
        alarmThreshold = DEFAULT_ALARM_THRESHOLD;
        scanStepKHz = SCAN_STEP_KHZ;
        activeModulation = MODULATION_TETRA;
        updateUIValues();
        saveSettings();
        updateFrequencyDisplay();
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        for (RTLSDRDevice device : connectedDevices) {
            device.disconnect();
        }
        connectedDevices.clear();
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
