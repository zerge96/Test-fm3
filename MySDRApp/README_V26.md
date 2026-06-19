# 📡 Frequenzy Monitor V26

**Professional Grade Frequency Scanner for Android with USB-C SDR**

## 🎯 Key Features

✅ **Fast Frequency Scanning**
- 380-385 MHz default range (configurable)
- <1 second complete scan
- 100 KHz resolution (adjustable)
- Real-time peak tracking

✅ **Audio Control**
- 🔇 Mute button (toggle)
- 🔔 Alarm enable/disable
- Adjustable alarm threshold
- Auto-volume management

✅ **Configuration**
- Save/load preferences
- Adjustable start/stop frequencies
- Threshold customization
- Step resolution control

✅ **Real-time Monitoring**
- Live signal strength display
- Progress indicator
- Peak frequency tracking
- Signal history graph

## 📋 Requirements

- Android 5.0+ (API 21)
- USB-C OTG cable
- RTL-SDR Dongle (or compatible SDR)
- Antenna

## 🚀 Quick Start

1. Connect USB-C SDR dongle
2. Launch app
3. Wait for "✅ SDR Connected"
4. Press "▶️ START SCAN"
5. Watch for peaks and alarms

## 🔧 Configuration Examples

### FM Radio Scan (88-108 MHz)
```
Start: 88 MHz
Stop: 108 MHz
Step: 100 KHz
Threshold: -40 dBm
```

### GSM Scan (900 MHz)
```
Start: 900 MHz
Stop: 910 MHz
Step: 100 KHz
Threshold: -60 dBm
```

### LTE Scan (2110 MHz)
```
Start: 2110 MHz
Stop: 2120 MHz
Step: 100 KHz
Threshold: -55 dBm
```

## 📊 UI Layout

```
┌─────────────────────────────┐
│ 📡 Frequenzy Monitor V26    │
│              🔔  🔊         │
├─────────────────────────────┤
│ Status: Ready               │
│ Range: 380-385 MHz          │
├─────────────────────────────┤
│ 📊 Signal Strength          │
│ Current: -65.2 dBm          │
│ [████░░░░░░░░░░░░]          │
│ Peak: 382.45 MHz | -45 dBm  │
├─────────────────────────────┤
│ [████████████░░░░░░░] 75%   │
├─────────────────────────────┤
│ [▶️ START]  [⏹️ STOP]        │
├─────────────────────────────┤
│ ⚙️ Configuration             │
│ Start: [380] MHz            │
│ Stop:  [385] MHz            │
│ Threshold: [-50] dBm        │
│ Step: [100] KHz             │
│ [💾 SAVE]                   │
└─────────────────────────────┘
```

## 🎛️ Button Functions

| Button | Function |
|--------|----------|
| **▶️ START SCAN** | Begin frequency sweep |
| **⏹️ STOP** | Stop current scan |
| **🔔/🔕** | Toggle alarm on/off |
| **🔊/🔇** | Toggle audio mute |
| **💾 SAVE** | Save configuration |

## ⚙️ Settings

### Start Frequency
- Range: 1-6000 MHz
- Default: 380 MHz
- Saved in SharedPreferences

### Stop Frequency  
- Range: 1-6000 MHz
- Default: 385 MHz
- Saved in SharedPreferences

### Alarm Threshold
- Range: -100 to +20 dBm
- Default: -50 dBm
- Triggers when signal > threshold

### Scan Step
- Range: 1-1000 KHz
- Default: 100 KHz
- Affects resolution and speed

## 📈 Performance

**Scan Duration:**
- 380-385 MHz @ 100 KHz: ~850ms
- 380-385 MHz @ 50 KHz: ~950ms
- 380-385 MHz @ 200 KHz: ~500ms

**Memory Usage:**
- ~30 MB RAM
- ~50 MB storage

## 🔍 Supported SDR Devices

- **RTL-SDR** (0x0BDA:0x2832) ✅
- **RTL-SDR v3** (0x0BDA:0x2838) ✅
- **HackRF One** (0x1D50:0x6089) (with adapter)
- **BladeRF** (0x2048:0x3102) (with adapter)

## 🐛 Troubleshooting

### SDR Not Connected
- Check USB cable
- Verify OTG adapter
- Enable USB debugging
- Restart app

### No Signal Detected
- Verify antenna connected
- Check frequency range
- Adjust threshold lower
- Try different frequency

### Scanning Slow
- Reduce step size
- Lower scan range
- Reduce sample count

## 📝 Version History

**v2.6** - Current
- 🎯 Fast < 1 second scanning
- 🔇 Mute button added
- 🔔 Separate alarm toggle
- 📊 Peak tracking
- 💾 Settings persistence
- Default 380-385 MHz range

**v2.0** - Previous
- USB-C SDR support
- Basic frequency monitoring
- Alarm functionality

## 📄 License

Open Source | MIT License

## 👨‍💻 Author

zerge96 - 2026

---

**Repository:** https://github.com/zerge96/Test-fm3
