package com.example.frequencymonitor;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class RTLSDRDevice {
    private static final String TAG = "RTLSDRDevice";

    private final UsbDevice device;
    private final UsbManager usbManager;
    private final Context context;
    private boolean connected;
    private long centerFrequency = 380_000_000L;
    private int sampleRate = 2_400_000;
    private int gain = 450;

    public RTLSDRDevice(UsbDevice device, UsbManager usbManager, Context context) {
        this.device = device;
        this.usbManager = usbManager;
        this.context = context.getApplicationContext();
    }

    public boolean connect() {
        if (device == null) {
            Log.w(TAG, "USB device is null");
            return false;
        }

        if (usbManager != null && !usbManager.hasPermission(device)) {
            Log.w(TAG, "No USB permission for device: " + device.getDeviceName());
            return false;
        }

        connected = true;
        Log.d(TAG, "RTL-SDR connected: " + device.getDeviceName());
        return true;
    }

    public void disconnect() {
        connected = false;
        Log.d(TAG, "RTL-SDR disconnected");
    }

    public boolean isConnected() {
        return connected;
    }

    public void setSampleRate(int sampleRateHz) {
        this.sampleRate = sampleRateHz;
    }

    public void setGain(int gainDb) {
        this.gain = gainDb;
    }

    public void setCenterFrequency(long frequencyHz) {
        this.centerFrequency = frequencyHz;
    }

    public float readSignalStrength() {
        if (!connected) {
            return -100.0f;
        }

        // Demo fallback for emulation when no actual RTL-SDR hardware is attached.
        double baseNoise = -68.0d;
        double drift = Math.sin((centerFrequency / 1_000_000.0d) / 10.0d) * 6.0d;
        double random = (Math.random() * 12.0d) - 6.0d;
        return (float) (baseNoise + drift + random);
    }

    public long getCenterFrequency() {
        return centerFrequency;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getGain() {
        return gain;
    }
}
