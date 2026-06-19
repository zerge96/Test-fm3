import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class SignalDetectionActivity extends Activity {

    private static final String TAG = "SignalDetection";
    private static final float DEFAULT_START_FREQUENCY = 100.0f;
    private static final float DEFAULT_BANDWIDTH = 20.0f;
    private static final float DEFAULT_ALARM_THRESHOLD = -50.0f;
    private static final int TONE_DURATION_MS = 200;
    private static final int MIN_DELAY_MS = 100;
    private static final int MAX_DELAY_MS = 1000;

    private float startFrequency;
    private float bandwidth;
    private float alarmThreshold;
    private boolean isAlarmEnabled;
    private boolean isMonitoring;

    private TextView statusText;
    private TextView frequencyText;
    private Button startButton;
    private Button stopButton;
    private CheckBox alarmCheckBox;

    private ToneGenerator toneGenerator;
    private Handler handler;
    private Runnable alarmRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal_detection);

        // Initialize handler with main looper (prevents crashes)
        handler = new Handler(Looper.getMainLooper());

        // Initialize UI elements
        initializeUI();

        // Initialize variables with defaults
        initializeVariables();

        // Initialize ToneGenerator safely
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ToneGenerator", e);
            Toast.makeText(this, "Audio initialization failed", Toast.LENGTH_SHORT).show();
        }

        // Display initial message
        displayWelcomeMessage();
    }

    private void initializeUI() {
        statusText = findViewById(R.id.statusText);
        frequencyText = findViewById(R.id.frequencyText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        alarmCheckBox = findViewById(R.id.alarmCheckBox);

        // Check if views exist before setting listeners
        if (startButton != null) {
            startButton.setOnClickListener(v -> startMonitoring());
        }
        if (stopButton != null) {
            stopButton.setOnClickListener(v -> stopMonitoring());
        }
        if (alarmCheckBox != null) {
            alarmCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> 
                isAlarmEnabled = isChecked);
        }
    }

    private void initializeVariables() {
        startFrequency = DEFAULT_START_FREQUENCY;
        bandwidth = DEFAULT_BANDWIDTH;
        alarmThreshold = DEFAULT_ALARM_THRESHOLD;
        isAlarmEnabled = true;
        isMonitoring = false;
    }

    private void displayWelcomeMessage() {
        String message = String.format(
            "Monitoring signal from %.2f MHz to %.2f MHz with alarm threshold at %.2f dBm.",
            startFrequency,
            startFrequency + bandwidth,
            alarmThreshold
        );
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        updateFrequencyDisplay();
    }

    private void updateFrequencyDisplay() {
        if (frequencyText != null) {
            String freqInfo = String.format("Range: %.2f - %.2f MHz\nThreshold: %.2f dBm",
                startFrequency, startFrequency + bandwidth, alarmThreshold);
            frequencyText.setText(freqInfo);
        }
    }

    private void startMonitoring() {
        isMonitoring = true;
        if (statusText != null) {
            statusText.setText("Monitoring started...");
        }
        if (startButton != null) startButton.setEnabled(false);
        if (stopButton != null) stopButton.setEnabled(true);
        
        // Simulate signal detection
        simulateSignalDetection(-60.0f);
    }

    private void stopMonitoring() {
        isMonitoring = false;
        
        // Stop alarm tone
        if (alarmRunnable != null) {
            handler.removeCallbacks(alarmRunnable);
        }
        
        if (statusText != null) {
            statusText.setText("Monitoring stopped.");
        }
        if (startButton != null) startButton.setEnabled(true);
        if (stopButton != null) stopButton.setEnabled(false);
    }

    private void simulateSignalDetection(float signalStrength) {
        if (!isMonitoring) return;

        String statusMessage;
        if (signalStrength > alarmThreshold) {
            statusMessage = String.format("⚠ ALARM: Signal detected at %.2f dBm", signalStrength);
            if (isAlarmEnabled && toneGenerator != null) {
                playAlarmTone(signalStrength);
            }
        } else {
            statusMessage = String.format("✓ No alarm: Signal at %.2f dBm (below threshold)", signalStrength);
        }

        if (statusText != null) {
            statusText.setText(statusMessage);
        }
        Log.d(TAG, statusMessage);
    }

    private void playAlarmTone(float signalStrength) {
        if (toneGenerator == null) {
            Log.w(TAG, "ToneGenerator is null");
            return;
        }

        // Calculate delay: stronger signal = faster beeping
        int delay = calculateAlarmDelay(signalStrength);

        alarmRunnable = () -> {
            if (isAlarmEnabled && isMonitoring && toneGenerator != null) {
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, TONE_DURATION_MS);
                    // Reschedule the alarm
                    playAlarmTone(signalStrength);
                } catch (Exception e) {
                    Log.e(TAG, "Error playing alarm tone", e);
                }
            }
        };

        handler.postDelayed(alarmRunnable, delay);
    }

    private int calculateAlarmDelay(float signalStrength) {
        // Stronger signal = shorter delay (more frequent beeps)
        // Formula: delay = (-signalStrength + 80) * 10, bounded between MIN and MAX
        int calculatedDelay = (int) ((-signalStrength + 80) * 10);
        return Math.max(MIN_DELAY_MS, Math.min(MAX_DELAY_MS, calculatedDelay));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop monitoring
        stopMonitoring();
        
        // Release ToneGenerator resources
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        
        // Remove all pending callbacks
        if (handler != null && alarmRunnable != null) {
            handler.removeCallbacks(alarmRunnable);
        }
        
        Log.d(TAG, "Activity destroyed and resources released");
    }

    // Setter methods for configuration
    public void setFrequencyRange(float start, float bw) {
        this.startFrequency = start;
        this.bandwidth = bw;
        updateFrequencyDisplay();
    }

    public void setAlarmThreshold(float threshold) {
        this.alarmThreshold = threshold;
        updateFrequencyDisplay();
    }
}
