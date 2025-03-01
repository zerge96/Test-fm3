java

package com.example.frequencymonitor;

 

import android.content.BroadcastReceiver;

import android.content.Context;

import android.content.Intent;

import android.hardware.usb.UsbDevice;

import android.widget.Toast;

 

public class UsbBroadcastReceiver extends BroadcastReceiver {

 

    @Override

    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if ("com.android.example.USB_PERMISSION".equals(action)) {

            UsbDevice device = intent.getParcelableExtra("device");

 

            if (intent.getBooleanExtra("permission", false)) {

                Toast.makeText(context, "USB permission granted for device: " + device.getDeviceName(), Toast.LENGTH_SHORT).show();

            } else {

                Toast.makeText(context, "USB permission denied for device: " + device.getDeviceName(), Toast.LENGTH_SHORT).show();

            }

        }

    }

}
