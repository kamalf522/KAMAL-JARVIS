package com.kamal.jarvis;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class AndroidControlEngine {

    private final Context context;

    public AndroidControlEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public String openSettings() {
        return launch(
                new Intent(Settings.ACTION_SETTINGS),
                "الإعدادات"
        );
    }

    public String openWifiSettings() {
        return launch(
                new Intent(Settings.ACTION_WIFI_SETTINGS),
                "إعدادات Wi-Fi"
        );
    }

    public String openBluetoothSettings() {
        return launch(
                new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                "إعدادات Bluetooth"
        );
    }

    public String openApplicationSettings() {
        return launch(
                new Intent(Settings.ACTION_APPLICATION_SETTINGS),
                "إعدادات التطبيقات"
        );
    }

    // Compatibility alias
    public String openAppSettings() {
        return openApplicationSettings();
    }

    public String openDeviceInformation() {
        return launch(
                new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
                "معلومات الجهاز"
        );
    }

    // Compatibility alias
    public String openDeviceInfo() {
        return openDeviceInformation();
    }

    public String openNotificationSettings() {
        return launch(
                new Intent(Settings.ACTION_NOTIFICATION_SETTINGS),
                "إعدادات الإشعارات"
        );
    }

    public String openDisplaySettings() {
        return launch(
                new Intent(Settings.ACTION_DISPLAY_SETTINGS),
                "إعدادات الشاشة"
        );
    }

    public String openSoundSettings() {
        return launch(
                new Intent(Settings.ACTION_SOUND_SETTINGS),
                "إعدادات الصوت"
        );
    }

    public String openBatterySettings() {
        return launch(
                new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                "إعدادات البطارية"
        );
    }

    public String getStatus() {
        if (context != null) {
            return "Android Control Engine: ONLINE ✓";
        }

        return "Android Control Engine: ERROR ⚠";
    }

    public boolean isHealthy() {
        return context != null;
    }

    private String launch(Intent intent, String name) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

            return "تم فتح " + name + " ✓";

        } catch (Exception e) {
            return "ما قدرتش نفتح " + name + " ⚠";
        }
    }
}