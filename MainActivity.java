import android.app.Activity;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class SignalDetectionActivity extends Activity {

    private float startFrequency;
    private float bandwidth;
    private float alarmThreshold;
    private boolean isAlarmEnabled;
    private TextView statusText;
    private ToneGenerator toneGenerator;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal_detection);

        // Initialize variables (this is just an example)
        startFrequency = 100.0f;
        bandwidth = 20.0f;
        alarmThreshold = -50.0f;
        isAlarmEnabled = true; // Assume alarm is enabled
        statusText = findViewById(R.id.statusText);
        toneGenerator = new ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
        handler = new Handler();

        // Display initial message
        String message = String.format("Monitoring signal from %.2f MHz to %.2f MHz with alarm threshold at %.2f dBm.", startFrequency, bandwidth, alarmThreshold);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Simulate a signal for the alarm
        simulateSignalDetection(-60.0f); // Example: Signal strength -60 dBm
    }

    private void simulateSignalDetection(float signalStrength) {
        if (signalStrength > alarmThreshold) { // Alarm condition
            String alarmMessage = String.format("ALARM: Signal detected at %.2f dBm", signalStrength);
            statusText.setText(alarmMessage);

            if (isAlarmEnabled) {
                playAlarmTone(signalStrength);
            }
        }
    }

    private void playAlarmTone(float signalStrength) {
        int delay = Math.max(100, (int) ((-signalStrength + 80) * 10)); // The stronger the signal, the shorter the delay
        handler.postDelayed(() -> {
            if (isAlarmEnabled) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                playAlarmTone(signalStrength); // Repeat the tone
            }
        }, delay);
    }
}
