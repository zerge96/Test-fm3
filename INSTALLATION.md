# 📱 Installation Guide - Frequenzy Monitor V26

## 🚀 Quick Installation (One-Click)

### Option 1: Direct Download from GitHub Releases ⭐ EASIEST
1. Go to: https://github.com/zerge96/Test-fm3/releases
2. Download the latest `frequenzy-monitor-v26.apk`
3. Open the APK file on your Android phone
4. Tap "Install"
5. Done! ✅

### Option 2: Build from Source

**Requirements:**
- Android Studio 2022.1+
- Java 11+
- Gradle 7.0+

**Steps:**
```bash
git clone https://github.com/zerge96/Test-fm3.git
cd Test-fm3
./gradlew assembleRelease
```

The APK will be generated at:
```
MySDRApp/build/outputs/apk/release/MySDRApp-release.apk
```

### Option 3: Automated Build via GitHub Actions
1. Push to `main` branch or create a tag
2. GitHub Actions automatically builds APK
3. Download from **Releases** or **Artifacts**

## 📱 Enable Unknown Sources (First Time)

If you get "Unknown App" warning:

### Android 12+
1. Settings → Apps → Special app access → Install unknown apps
2. Select your browser/file manager → Allow

### Android 11 and earlier
1. Settings → Security → Unknown Sources → Toggle ON

## ✅ Permissions Required

The app needs:
- ✅ **USB access** (for SDR dongle)
- ✅ **Audio access** (for alarms)

**Grant permissions when app first starts!**

## 🔧 Hardware Requirements

- **Minimum:** Android 5.0+ (API 21)
- **Recommended:** Android 8.0+
- **USB:** USB-C OTG cable
- **SDR:** RTL-SDR Dongle or compatible device
- **Antenna:** External antenna recommended

## 📋 Pre-Installation Checklist

- [ ] Android device with USB-C or micro-USB port
- [ ] USB OTG adapter/cable
- [ ] RTL-SDR dongle
- [ ] Antenna
- [ ] USB debugging enabled (Settings → Developer Options)

## 🐛 Troubleshooting

### "App not installed"
**Solution:**
- Update Android to 5.0 or higher
- Clear Play Store cache: Settings → Apps → Play Store → Storage → Clear Cache
- Free up at least 50MB storage

### "Unknown sources blocked"
**Solution:**
- Enable installation from Unknown Sources (see above)
- Use different browser to download APK

### "SDR not detected"
**Solution:**
1. Connect USB-C OTG cable to phone
2. Connect RTL-SDR dongle to OTG cable
3. Grant USB permissions when prompted
4. Restart app
5. Tap "Refresh" button in app

### "App crashes on startup"
**Solution:**
- Restart phone
- Uninstall and reinstall app
- Check if Android version is 5.0+

### "No signal detected"
**Solution:**
- Check antenna is connected to SDR
- Try different frequency range
- Lower threshold value
- Move away from interference sources

## 📥 File Locations

After download, APK file is usually in:
- **Chrome:** `Downloads/frequenzy-monitor-v26.apk`
- **Firefox:** `Downloads/frequenzy-monitor-v26.apk`
- **File Manager:** Check Downloads folder

## 🔗 Useful Links

- **GitHub Repository:** https://github.com/zerge96/Test-fm3
- **Releases:** https://github.com/zerge96/Test-fm3/releases
- **Report Issues:** https://github.com/zerge96/Test-fm3/issues
- **Android Support:** https://support.google.com/android

## 📞 Support

If you encounter issues:
1. Check **Troubleshooting** section above
2. Open an issue on GitHub: https://github.com/zerge96/Test-fm3/issues
3. Include Android version, device model, and error message

---

**Version:** 2.6 | **Author:** zerge96 | **License:** MIT | **Last Updated:** 2026-06-19
