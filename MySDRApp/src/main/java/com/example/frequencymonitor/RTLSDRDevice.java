package com.example.frequencymonitor;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class RTLSDRDevice {
    private static final String TAG = "RTLSDRDevice";
    private static final int[] SUPPORTED_PRODUCT_IDS = {
            0x2832,
            0x2838,
            0x2880,
            0x2881,
            0x2882,
            0x2883
    };

    public static final int MAX_BANDWIDTH_HZ = 2_500_000;

    private final UsbDevice usbDevice;
    private final UsbManager usbManager;
    private final Context appContext;
    private boolean connected = false;
    private boolean hardwareReady = false;
    private long centerFrequency = 380_000_000L;
    private int sampleRate = 2_400_000;
    private int gain = 450;

    public RTLSDRDevice(UsbDevice usbDevice, UsbManager usbManager) {
        this(usbDevice, usbManager, null);
    }

    public RTLSDRDevice(UsbDevice usbDevice, UsbManager usbManager, Context appContext) {
        this.usbDevice = usbDevice;
        this.usbManager = usbManager;
        this.appContext = appContext;
    }

    public static boolean isSupportedDevice(UsbDevice device) {
        if (device == null) {
            return false;
        }

        if (device.getVendorId() != 0x0bda) {
            return false;
        }

        for (int productId : SUPPORTED_PRODUCT_IDS) {
            if (productId == device.getProductId()) {
                return true;
            }
        }
        return false;
    }

    public boolean connect() {
        if (usbDevice == null) {
            Log.w(TAG, "USB device is null");
            return false;
        }

        if (usbManager != null && !usbManager.hasPermission(usbDevice)) {
            Log.w(TAG, "No USB permission for device: " + usbDevice.getDeviceName());
            return false;
        }

        connected = true;
        hardwareReady = true;
        Log.d(TAG, "RTL-SDR USB device detected: " + usbDevice.getDeviceName());
        return true;
    }

    public void disconnect() {
        connected = false;
        hardwareReady = false;
        Log.d(TAG, "RTL-SDR disconnected");
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isHardwareReady() {
        return connected && hardwareReady;
    }

    public void setHardwareReady(boolean ready) {
        this.hardwareReady = connected && ready;
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
            return Float.NaN;
        }
        if (!hardwareReady) {
            Log.w(TAG, "RTL-SDR device connected but native hardware bridge is not active yet.");
            return Float.NaN;
        }

        // The app is intentionally structured to remain stable while the native SDR bridge is connected.
        // Real hardware data acquisition can be introduced in a later step without breaking the UI.
        return Float.NaN;
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

    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    public Context getAppContext() {
        return appContext;
    }
}
