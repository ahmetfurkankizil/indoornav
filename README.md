# ORB-SLAM3 Phone Camera Demo

This project enables you to run ORB-SLAM3 visual SLAM using your Android phone's camera, with all processing done on your PC. The phone streams video over WiFi to WSL2, where ORB-SLAM3 processes it in real-time.

## Overview

```
┌──────────────┐     WiFi      ┌──────────────────────────────────┐
│   Android    │ ────────────> │           Windows PC             │
│    Phone     │  HTTP Stream  │  ┌────────────────────────────┐  │
│  (IP Webcam) │               │  │          WSL2              │  │
└──────────────┘               │  │  ┌────────────────────┐    │  │
                               │  │  │    ORB-SLAM3       │    │  │
                               │  │  │  - Feature Extract │    │  │
                               │  │  │  - Tracking        │    │  │
                               │  │  │  - Mapping         │    │  │
                               │  │  │  - Visualization   │    │  │
                               │  │  └────────────────────┘    │  │
                               │  └────────────────────────────┘  │
                               └──────────────────────────────────┘
```

## Requirements

### Hardware
- Android phone with camera
- Windows 10/11 PC with WSL2
- Both devices on the same WiFi network

### Software
- WSL2 with Ubuntu 22.04
- IP Webcam app (Android, free on Google Play)

## Quick Start

### Step 1: Install WSL2 Dependencies

Open Ubuntu WSL2 terminal and run:

```bash
chmod +x setup_wsl.sh
./setup_wsl.sh
```

This installs all required libraries (~10-15 minutes).

### Step 2: Build ORB-SLAM3 and Dependencies

```bash
chmod +x build_deps.sh
./build_deps.sh
```

This builds Pangolin, OpenCV, and ORB-SLAM3 (~30-60 minutes depending on CPU).

### Step 3: Set Up Phone Camera

1. Install **IP Webcam** from Google Play Store on your Android phone
2. Open the app
3. (Optional) Adjust settings:
   - Video preferences → Resolution: 720p or 1080p
   - Video preferences → Quality: 50-70%
4. Scroll to bottom, tap **Start server**
5. Note the IP address (e.g., `http://192.168.1.100:8080`)

### Step 4: Test the Stream

```bash
chmod +x test_stream.sh
./test_stream.sh http://192.168.1.100:8080/video
```

Replace the IP with your phone's IP address.

### Step 5: Calibrate Your Camera

For accurate SLAM, calibrate your phone's camera:

1. Print a checkerboard pattern (9x6 inner corners)
   - Download from: https://docs.opencv.org/4.x/pattern.png
2. Run calibration:

```bash
python3 calibrate_camera.py --url http://192.168.1.100:8080/video
```

3. Follow on-screen instructions to capture 20 images
4. This generates `phone_camera.yaml` with your camera parameters

### Step 6: Build the Demo

```bash
chmod +x build_phone_demo.sh
./build_phone_demo.sh
```

### Step 7: Run the Demo!

```bash
chmod +x run_demo.sh
./run_demo.sh http://192.168.1.100:8080/video
```

## Controls

| Key | Action |
|-----|--------|
| `ESC` or `q` | Quit |
| `SPACE` | Pause/Resume tracking |
| `s` | Save current map |
| `r` | Reset SLAM system |

## Tips for Good Tracking

1. **Move slowly** - Fast motion causes blur and tracking loss
2. **Point at textures** - SLAM needs visual features (avoid blank walls, sky)
3. **Good lighting** - Avoid dark areas or harsh backlighting
4. **Loop closure** - Return to previously seen areas to improve accuracy
5. **Initial mapping** - Move slowly at first to build a good initial map

## Troubleshooting

### "Cannot connect to video stream"

1. Check IP Webcam is running on your phone
2. Verify phone and PC are on the same WiFi network
3. Try accessing `http://<phone-ip>:8080` in a browser
4. Check Windows Firewall isn't blocking WSL2

### "Tracking lost"

1. Move the camera more slowly
2. Point at textured surfaces
3. Return to previously mapped areas
4. Press `r` to reset if needed

### Build errors

```bash
# Update library cache
sudo ldconfig

# Check OpenCV installation
pkg-config --modversion opencv4

# Check Pangolin
ls /usr/local/lib/libpangolin*
```

### Low FPS

1. Reduce phone camera resolution (720p instead of 1080p)
2. Lower video quality in IP Webcam settings
3. Reduce `ORBextractor.nFeatures` in `phone_camera.yaml`

## File Structure

```
orbslam3-phone-demo/
├── setup_wsl.sh          # Install WSL2 dependencies
├── build_deps.sh         # Build Pangolin, OpenCV, ORB-SLAM3
├── build_phone_demo.sh   # Build the phone demo
├── run_demo.sh           # Run the demo
├── test_stream.sh        # Test phone stream connection
├── calibrate_camera.py   # Camera calibration tool
├── phone_camera.yaml     # Camera settings template
├── phone_mono.cc         # Main demo source code
├── CMakeLists.txt        # Build configuration
└── README.md             # This file
```

## Output Files

After running the demo:

- `phone_trajectory_tum.txt` - Camera trajectory in TUM format
- `phone_keyframes_tum.txt` - Keyframe trajectory
- `phone_slam_map.osa` - Saved map (if you pressed 's')

## Advanced Usage

### Custom Camera Settings

Edit `phone_camera.yaml` to adjust:

- `ORBextractor.nFeatures` - More features = better tracking but slower (default: 1500)
- `ORBextractor.scaleFactor` - Scale pyramid factor (default: 1.2)
- `Camera.fps` - Expected frame rate

### Recording Video

IP Webcam can record video while streaming:
1. Tap **Actions** → **Record video**
2. The video is saved on your phone
3. You can process recorded videos with ORB-SLAM3 later

### Different Camera Modes

This demo uses monocular mode. ORB-SLAM3 also supports:
- **Monocular-Inertial** - Uses phone's IMU for better tracking
- **Stereo** - Requires two cameras
- **RGB-D** - Requires depth camera

## References

- [ORB-SLAM3 GitHub](https://github.com/UZ-SLAMLab/ORB_SLAM3)
- [ORB-SLAM3 Paper](https://arxiv.org/abs/2007.11898)
- [IP Webcam App](https://play.google.com/store/apps/details?id=com.pas.webcam)
- [OpenCV Camera Calibration](https://docs.opencv.org/4.x/dc/dbb/tutorial_py_calibration.html)

## License

This demo code is provided for educational purposes. ORB-SLAM3 is licensed under GPLv3.

