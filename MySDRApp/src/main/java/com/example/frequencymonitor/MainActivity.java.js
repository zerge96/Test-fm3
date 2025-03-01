java
package com.example.frequencymonitor;

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbManager usbManager;
    private TextView statusText;
    private EditText startFrequencyInput, bandwidthInput;
    private Button startMonitoringButton;
    private View muteButton;

    private float startFrequency = 140.0f; // Standard-Startfrequenz (MHz)
    private float bandwidth = 5.0f; // Standard-Bandbreite (MHz)
    private boolean isAlarmEnabled = true; // Alarmton standardmäßig eingeschaltet
    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // USB-Manager initialisieren
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        statusText = findViewById(R.id.statusText);
        startFrequencyInput = findViewById(R.id.startFrequencyInput);
        bandwidthInput = findViewById(R.id.bandwidthInput);
        startMonitoringButton = findViewById(R.id.startMonitoringButton);
        muteButton = findViewById(R.id.muteButton);

        // Button-Listener für das Starten der Überwachung
        startMonitoringButton.setOnClickListener(v -> {
            if (validateInputs()) {
                startMonitoring();
            }
        });

        // Listener für den Stummschalt-Knopf
        muteButton.setOnClickListener(v -> toggleAlarm());
    }

    private void toggleAlarm() {
        isAlarmEnabled = !isAlarmEnabled; // Zustand umschalten

        // Ändere die Farbe des Kreises
        if (isAlarmEnabled) {
            muteButton.setBackgroundResource(R.drawable.circle_green); // Grün: Ton eingeschaltet
        } else {
            muteButton.setBackgroundResource(R.drawable.circle_red); // Rot: Ton ausgeschaltet
        }
    }

    private boolean validateInputs() {
        try {
            // Lese die Eingaben aus den EditText-Feldern
            startFrequency = Float.parseFloat(startFrequencyInput.getText().toString());
            bandwidth = Float.parseFloat(bandwidthInput.getText().toString());

            if (startFrequency <= 0 || bandwidth <= 0) {
                Toast.makeText(this, "Please enter valid positive numbers.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input. Please enter numbers only.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void startMonitoring() {
        // Starte die Überwachung mit den eingegebenen Werten
        String message = String.format("Monitoring started at %.2f MHz with %.2f MHz bandwidth.", startFrequency, bandwidth);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Simuliere ein Signal für den Alarm
        simulateSignalDetection(-60.0f); // Beispiel: Signalstärke -60 dBm
    }

    private void simulateSignalDetection(float signalStrength) {
        if (signalStrength > -65.0f) { // Alarmbedingung
            String alarmMessage = String.format("ALARM: Signal detected at %.2f dBm", signalStrength);
            statusText.setText(alarmMessage);

            if (isAlarmEnabled) {
                playAlarmTone(signalStrength);
            }
        }
    }

    private void playAlarmTone(float signalStrength) {
        int delay = Math.max(100, (int) ((-signalStrength + 80) * 10)); // Je stärker das Signal, desto kürzer der Delay
        handler.postDelayed(() -> {
            if (isAlarmEnabled) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                playAlarmTone(signalStrength); // Wiederhole den Ton
            }
        }, delay);
    }
}