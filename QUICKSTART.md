# ORB-SLAM3 Phone Live Demo - Quick Start Guide

This guide will help you run ORB-SLAM3 with your Samsung Galaxy S23 FE.

## Prerequisites

- ✅ ORB-SLAM3 built (in `/home/lkilic/IMU/ORB_SLAM3/`)
- ✅ Phone bridge built (in `/home/lkilic/IMU/phone_bridge/build/`)
- Your phone and PC on the same WiFi network

## Two Options Available

### Option 1: IP Webcam Method (Easier - Recommended to Start)

This uses the free IP Webcam app from Play Store.

#### Step 1: Install Apps on Your Phone
1. **IP Webcam** - from Google Play Store
2. **Sensor Server** (or **Wireless IMU**) - from Google Play Store

#### Step 2: Configure IP Webcam
1. Open IP Webcam app
2. Scroll down and tap "Start server"
3. Note the URL shown (e.g., `http://192.168.1.100:8080`)

#### Step 3: Configure Sensor Server
1. Open Sensor Server app
2. Enable "Accelerometer" and "Gyroscope"
3. Set target IP to your WSL2 IP (find it below)
4. Set port to `5001`
5. Set format to CSV or JSON
6. Start streaming

#### Step 4: Get Your WSL2 IP
```bash
ip addr show eth0 | grep "inet " | awk '{print $2}' | cut -d/ -f1
```

#### Step 5: Run ORB-SLAM3
```bash
cd /home/lkilic/IMU

# Set library paths
export LD_LIBRARY_PATH="/home/lkilic/IMU/ORB_SLAM3/lib:/home/lkilic/IMU/ORB_SLAM3/Thirdparty/DBoW2/lib:/home/lkilic/IMU/ORB_SLAM3/Thirdparty/g2o/lib:$LD_LIBRARY_PATH"

# Run with IP Webcam
./phone_bridge/build/phone_slam_ipwebcam \
    ORB_SLAM3/Vocabulary/ORBvoc.txt \
    configs/samsung_s23fe.yaml \
    "http://PHONE_IP:8080/video" \
    5001
```

Replace `PHONE_IP` with your phone's IP from IP Webcam.

---

### Option 2: Termux Method (Better Sync)

This uses Termux for synchronized camera + IMU streaming.

#### Step 1: Install Termux
1. Install **Termux** from F-Droid (NOT Play Store!)
2. Install **Termux:API** from F-Droid

#### Step 2: Setup Termux
Open Termux and run:
```bash
pkg update
pkg install python termux-api
pip install numpy
termux-setup-storage  # Grant storage permission
```

#### Step 3: Copy Script to Phone
Transfer `/home/lkilic/IMU/android/stream_sensors.py` to your phone.

You can use ADB:
```bash
# On Windows, download adb and run:
adb push /home/lkilic/IMU/android/stream_sensors.py /sdcard/

# Then in Termux:
cp /sdcard/stream_sensors.py ~/
```

#### Step 4: Run on Phone
In Termux:
```bash
python stream_sensors.py --host YOUR_WSL2_IP --camera-port 5000 --imu-port 5001
```

#### Step 5: Run ORB-SLAM3
```bash
cd /home/lkilic/IMU

export LD_LIBRARY_PATH="/home/lkilic/IMU/ORB_SLAM3/lib:/home/lkilic/IMU/ORB_SLAM3/Thirdparty/DBoW2/lib:/home/lkilic/IMU/ORB_SLAM3/Thirdparty/g2o/lib:$LD_LIBRARY_PATH"

./phone_bridge/build/phone_slam \
    ORB_SLAM3/Vocabulary/ORBvoc.txt \
    configs/samsung_s23fe.yaml \
    5000 \
    5001
```

---

## Network Configuration (Windows Firewall)

If your phone can't connect to WSL2, you need to configure Windows Firewall:

### Option A: Disable Firewall Temporarily
```powershell
# Run in PowerShell as Administrator on Windows
Set-NetFirewallProfile -Profile Private -Enabled False
```

### Option B: Add Firewall Rules (Recommended)
```powershell
# Run in PowerShell as Administrator
New-NetFirewallRule -DisplayName "WSL2 Camera" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "WSL2 IMU" -Direction Inbound -LocalPort 5001 -Protocol UDP -Action Allow
New-NetFirewallRule -DisplayName "WSL2 IP Webcam" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

### Option C: Port Forwarding
If still having issues, forward ports from Windows to WSL2:
```powershell
# In PowerShell as Administrator
$wslIp = wsl hostname -I
netsh interface portproxy add v4tov4 listenport=5000 listenaddress=0.0.0.0 connectport=5000 connectaddress=$wslIp
netsh interface portproxy add v4tov4 listenport=5001 listenaddress=0.0.0.0 connectport=5001 connectaddress=$wslIp
```

---

## Troubleshooting

### "Failed to open video stream"
- Check IP Webcam is running and the URL is correct
- Try opening the URL in a browser first
- Make sure your phone and PC are on the same network

### "No IMU data"
- Check Sensor Server is running and sending to correct IP/port
- Try running `nc -lup 5001` in WSL2 to test if data arrives

### "SLAM not initializing"
- Move the camera slowly with some rotation
- Make sure there are enough features in view (textured surfaces)
- Check camera calibration parameters

### "Connection refused"
- Check Windows Firewall settings
- Verify WSL2 IP address is correct
- Try the port forwarding option

---

## Calibration (Optional but Recommended)

For better accuracy, calibrate your camera:

1. Print a checkerboard pattern (9x6 inner corners)
2. Capture 15-20 images from different angles
3. Run calibration:
```bash
python /home/lkilic/IMU/scripts/calibrate_camera.py \
    --images ./calibration_images/ \
    --rows 6 --cols 9
```
4. Update `configs/samsung_s23fe.yaml` with the results

---

## Files Created

```
/home/lkilic/IMU/
├── ORB_SLAM3/              # ORB-SLAM3 library
├── Pangolin/               # Visualization library
├── phone_bridge/
│   ├── phone_slam          # Termux version executable
│   └── phone_slam_ipwebcam # IP Webcam version executable
├── configs/
│   └── samsung_s23fe.yaml  # Camera-IMU configuration
├── scripts/
│   ├── run_phone_slam.sh   # Helper script
│   └── calibrate_camera.py # Calibration tool
└── android/
    └── stream_sensors.py   # Termux streaming script
```

---

## Need Help?

1. Check that ORB-SLAM3 works with a dataset first
2. Test camera streaming separately before adding IMU
3. Verify network connectivity with ping

