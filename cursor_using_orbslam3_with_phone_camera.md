# Using ORBSLAM3 with phone camera
_Exported on 11/30/2025 at 14:26:58 GMT+3 from Cursor (2.1.39)_

---

**User**

I want to use ORBSLAM3 as a demo using my phoens camera but processing should be done on the PC. How to do it fully

---

**Cursor**

I'd be happy to help you set up ORB-SLAM3 with your phone's camera! Before I create a detailed plan, I need to clarify a few important details:

Excellent! With Android, WSL2, and monocular mode, here's what we'll set up:

---

**User**

Implement the plan as specified, it is attached for your reference. Do NOT edit the plan file itself.

To-do's from the plan have already been created. Do not create them again. Mark them as in_progress as you work, starting with the first one. Don't stop until you have completed all the to-dos.

---

**Cursor**

I'll implement the plan now. Let me start by marking the first todo as in progress and creating the setup scripts.

```shellscript
#!/bin/bash
#
# ORB-SLAM3 Phone Demo - WSL2 Setup Script
# This script installs all required dependencies for ORB-SLAM3 in Ubuntu WSL2
#

set -e  # Exit on error

echo "=========================================="
echo "ORB-SLAM3 Phone Demo - WSL2 Setup"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running in WSL
if ! grep -qi microsoft /proc/version 2>/dev/null; then
    print_warning "This script is designed for WSL2. Continuing anyway..."
fi

# Update package lists
print_status "Updating package lists..."
sudo apt update

# Install essential build tools
print_status "Installing essential build tools..."
sudo apt install -y \
    build-essential \
    cmake \
    git \
    pkg-config \
    wget \
    unzip

# Install OpenCV dependencies
print_status "Installing OpenCV dependencies..."
sudo apt install -y \
    libgtk2.0-dev \
    libgtk-3-dev \
    libavcodec-dev \
    libavformat-dev \
    libswscale-dev \
    libv4l-dev \
    libxvidcore-dev \
    libx264-dev \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    libatlas-base-dev \
    gfortran

# Install Pangolin dependencies
print_status "Installing Pangolin dependencies..."
sudo apt install -y \
    libglew-dev \
    libepoxy-dev \
    libegl1-mesa-dev \
    libwayland-dev \
    libxkbcommon-dev \
    wayland-protocols

# Install ORB-SLAM3 dependencies
print_status "Installing ORB-SLAM3 dependencies..."
sudo apt install -y \
    libeigen3-dev \
    libboost-all-dev \
    libssl-dev \
    libtbb2 \
    libtbb-dev

# Install Python for calibration script
print_status "Installing Python and OpenCV Python bindings..."
sudo apt install -y \
    python3 \
    python3-pip \
    python3-numpy \
    python3-opencv

# Install FFmpeg for testing video streams
print_status "Installing FFmpeg..."
sudo apt install -y ffmpeg

# Install additional useful tools
print_status "Installing additional tools..."
sudo apt install -y \
    htop \
    nano \
    curl

# Create workspace directory
WORKSPACE_DIR="$HOME/orbslam3_phone_demo"
print_status "Creating workspace directory: $WORKSPACE_DIR"
mkdir -p "$WORKSPACE_DIR"

# Update library cache
print_status "Updating library cache..."
sudo ldconfig

echo ""
echo "=========================================="
echo -e "${GREEN}WSL2 Setup Complete!${NC}"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Run ./build_deps.sh to build Pangolin, OpenCV, and ORB-SLAM3"
echo "  2. Calibrate your phone camera using calibrate_camera.py"
echo "  3. Run the demo with ./run_demo.sh"
echo ""
echo "Workspace directory: $WORKSPACE_DIR"
echo ""

```

```shellscript
#!/bin/bash
#
# ORB-SLAM3 Phone Demo - Build Dependencies Script
# This script builds Pangolin, OpenCV, and ORB-SLAM3 from source
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
WORKSPACE_DIR="$HOME/orbslam3_phone_demo"
NUM_CORES=$(nproc)

echo "=========================================="
echo "ORB-SLAM3 Phone Demo - Build Dependencies"
echo "=========================================="
echo ""
echo "Workspace: $WORKSPACE_DIR"
echo "Using $NUM_CORES CPU cores for compilation"
echo ""

mkdir -p "$WORKSPACE_DIR"
cd "$WORKSPACE_DIR"

# ==========================================
# Build Pangolin
# ==========================================
print_step "Building Pangolin (visualization library)..."

if [ -d "Pangolin" ]; then
    print_warning "Pangolin directory exists. Skipping clone..."
else
    git clone --depth 1 https://github.com/stevenlovegrove/Pangolin.git
fi

cd Pangolin
mkdir -p build
cd build

cmake .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_TESTS=OFF

make -j$NUM_CORES
sudo make install
sudo ldconfig

cd "$WORKSPACE_DIR"
print_status "Pangolin build complete!"
echo ""

# ==========================================
# Build OpenCV 4.x
# ==========================================
print_step "Building OpenCV 4.x..."

OPENCV_VERSION="4.8.0"

if [ -d "opencv" ]; then
    print_warning "OpenCV directory exists. Skipping clone..."
else
    git clone --depth 1 --branch $OPENCV_VERSION https://github.com/opencv/opencv.git
fi

if [ -d "opencv_contrib" ]; then
    print_warning "OpenCV contrib directory exists. Skipping clone..."
else
    git clone --depth 1 --branch $OPENCV_VERSION https://github.com/opencv/opencv_contrib.git
fi

cd opencv
mkdir -p build
cd build

cmake .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX=/usr/local \
    -DOPENCV_EXTRA_MODULES_PATH=../../opencv_contrib/modules \
    -DWITH_TBB=ON \
    -DWITH_V4L=ON \
    -DWITH_OPENGL=ON \
    -DWITH_FFMPEG=ON \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_TESTS=OFF \
    -DBUILD_PERF_TESTS=OFF \
    -DBUILD_opencv_python3=ON

make -j$NUM_CORES
sudo make install
sudo ldconfig

cd "$WORKSPACE_DIR"
print_status "OpenCV build complete!"
echo ""

# ==========================================
# Build ORB-SLAM3
# ==========================================
print_step "Building ORB-SLAM3..."

if [ -d "ORB_SLAM3" ]; then
    print_warning "ORB_SLAM3 directory exists. Skipping clone..."
else
    git clone https://github.com/UZ-SLAMLab/ORB_SLAM3.git
fi

cd ORB_SLAM3

# Apply patches for compatibility with newer compilers if needed
print_status "Applying compatibility patches..."

# Fix for C++14/17 compatibility - update CMakeLists.txt
if ! grep -q "CMAKE_CXX_STANDARD 14" CMakeLists.txt; then
    sed -i 's/set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")/set(CMAKE_CXX_STANDARD 14)\nset(CMAKE_CXX_STANDARD_REQUIRED ON)/' CMakeLists.txt
fi

# Build the vocabulary (this is pre-built but needs extraction)
if [ -f "Vocabulary/ORBvoc.txt.tar.gz" ]; then
    print_status "Extracting vocabulary..."
    cd Vocabulary
    tar -xf ORBvoc.txt.tar.gz
    cd ..
fi

# Build Thirdparty libraries
print_status "Building DBoW2..."
cd Thirdparty/DBoW2
mkdir -p build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$NUM_CORES
cd ../../..

print_status "Building g2o..."
cd Thirdparty/g2o
mkdir -p build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$NUM_CORES
cd ../../..

print_status "Building Sophus..."
cd Thirdparty/Sophus
mkdir -p build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$NUM_CORES
cd ../../..

# Build ORB-SLAM3 main library
print_status "Building ORB-SLAM3 main library..."
mkdir -p build
cd build
cmake .. \
    -DCMAKE_BUILD_TYPE=Release
make -j$NUM_CORES
cd ..

print_status "ORB-SLAM3 build complete!"
echo ""

# ==========================================
# Create symlinks for easy access
# ==========================================
print_step "Creating symlinks..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create symlink to ORB_SLAM3 in project directory
if [ -L "$SCRIPT_DIR/ORB_SLAM3" ]; then
    rm "$SCRIPT_DIR/ORB_SLAM3"
fi
ln -sf "$WORKSPACE_DIR/ORB_SLAM3" "$SCRIPT_DIR/ORB_SLAM3"

echo ""
echo "=========================================="
echo -e "${GREEN}All Dependencies Built Successfully!${NC}"
echo "=========================================="
echo ""
echo "ORB-SLAM3 location: $WORKSPACE_DIR/ORB_SLAM3"
echo ""
echo "Next steps:"
echo "  1. Calibrate your phone camera: python3 calibrate_camera.py"
echo "  2. Copy phone_mono.cc to ORB_SLAM3/Examples/Monocular/"
echo "  3. Build the phone demo: ./build_phone_demo.sh"
echo "  4. Run the demo: ./run_demo.sh"
echo ""

```

```python
#!/usr/bin/env python3
"""
ORB-SLAM3 Phone Demo - Camera Calibration Tool

This script calibrates your phone camera using a checkerboard pattern.
It captures images from an IP Webcam stream and computes the camera matrix
and distortion coefficients needed for ORB-SLAM3.

Usage:
    python3 calibrate_camera.py --url http://192.168.1.100:8080/video
    python3 calibrate_camera.py --url http://192.168.1.100:8080/video --rows 9 --cols 6
"""

import argparse
import cv2
import numpy as np
import os
import sys
import time
import yaml
from datetime import datetime


class CameraCalibrator:
    def __init__(self, stream_url, checkerboard_rows=9, checkerboard_cols=6, 
                 square_size=0.025, num_images=20):
        """
        Initialize the camera calibrator.
        
        Args:
            stream_url: URL of the IP Webcam video stream
            checkerboard_rows: Number of inner corners in rows
            checkerboard_cols: Number of inner corners in columns
            square_size: Size of each square in meters (default 2.5cm)
            num_images: Number of calibration images to capture
        """
        self.stream_url = stream_url
        self.checkerboard_size = (checkerboard_cols, checkerboard_rows)
        self.square_size = square_size
        self.num_images = num_images
        
        # Calibration data storage
        self.obj_points = []  # 3D points in real world space
        self.img_points = []  # 2D points in image plane
        self.image_size = None
        
        # Prepare object points (0,0,0), (1,0,0), (2,0,0), ..., (cols-1,rows-1,0)
        self.objp = np.zeros((checkerboard_rows * checkerboard_cols, 3), np.float32)
        self.objp[:, :2] = np.mgrid[0:checkerboard_cols, 0:checkerboard_rows].T.reshape(-1, 2)
        self.objp *= square_size
        
        # Calibration results
        self.camera_matrix = None
        self.dist_coeffs = None
        self.rvecs = None
        self.tvecs = None
        
        # Create output directory
        self.output_dir = "calibration_images"
        os.makedirs(self.output_dir, exist_ok=True)

    def connect_to_stream(self):
        """Connect to the IP Webcam stream."""
        print(f"\nConnecting to stream: {self.stream_url}")
        print("Please make sure IP Webcam is running on your phone...")
        
        self.cap = cv2.VideoCapture(self.stream_url)
        
        if not self.cap.isOpened():
            print("\nError: Could not connect to video stream!")
            print("\nTroubleshooting tips:")
            print("  1. Make sure IP Webcam app is running on your phone")
            print("  2. Verify the IP address and port are correct")
            print("  3. Ensure your phone and PC are on the same network")
            print("  4. Try accessing the stream in a browser first")
            return False
        
        # Get stream properties
        ret, frame = self.cap.read()
        if ret:
            self.image_size = (frame.shape[1], frame.shape[0])
            print(f"Connected! Stream resolution: {self.image_size[0]}x{self.image_size[1]}")
            return True
        
        return False

    def capture_calibration_images(self):
        """Interactive capture of calibration images."""
        print("\n" + "="*60)
        print("CALIBRATION IMAGE CAPTURE")
        print("="*60)
        print(f"\nCheckerboard pattern: {self.checkerboard_size[0]}x{self.checkerboard_size[1]} inner corners")
        print(f"Need to capture: {self.num_images} images")
        print("\nInstructions:")
        print("  - Hold the checkerboard pattern in front of the camera")
        print("  - Move it to different positions and angles")
        print("  - Press SPACE to capture when corners are detected (green)")
        print("  - Press 'q' to quit early (if enough images captured)")
        print("  - Press 'r' to reset and start over")
        print("\n")
        
        captured_count = 0
        
        while captured_count < self.num_images:
            ret, frame = self.cap.read()
            if not ret:
                print("Error: Lost connection to stream!")
                break
            
            # Convert to grayscale for corner detection
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            
            # Find checkerboard corners
            flags = cv2.CALIB_CB_ADAPTIVE_THRESH + cv2.CALIB_CB_NORMALIZE_IMAGE
            ret_corners, corners = cv2.findChessboardCorners(gray, self.checkerboard_size, flags)
            
            # Create display frame
            display = frame.copy()
            
            # Draw status
            status_color = (0, 255, 0) if ret_corners else (0, 0, 255)
            status_text = "PATTERN DETECTED - Press SPACE to capture" if ret_corners else "Pattern not detected"
            cv2.putText(display, status_text, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, status_color, 2)
            cv2.putText(display, f"Captured: {captured_count}/{self.num_images}", (10, 60), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
            
            # Draw corners if found
            if ret_corners:
                # Refine corner positions
                criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
                corners_refined = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
                cv2.drawChessboardCorners(display, self.checkerboard_size, corners_refined, ret_corners)
            
            # Show frame
            cv2.imshow('Camera Calibration', display)
            
            key = cv2.waitKey(1) & 0xFF
            
            if key == ord(' ') and ret_corners:
                # Capture this frame
                criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
                corners_refined = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
                
                self.obj_points.append(self.objp)
                self.img_points.append(corners_refined)
                
                # Save image
                img_path = os.path.join(self.output_dir, f"calib_{captured_count:02d}.jpg")
                cv2.imwrite(img_path, frame)
                
                captured_count += 1
                print(f"  Captured image {captured_count}/{self.num_images}")
                
                # Flash effect
                flash = np.ones_like(frame) * 255
                cv2.imshow('Camera Calibration', flash)
                cv2.waitKey(100)
                
            elif key == ord('q'):
                if captured_count >= 5:
                    print("\nQuitting early with", captured_count, "images")
                    break
                else:
                    print("\nNeed at least 5 images for calibration!")
                    
            elif key == ord('r'):
                print("\nResetting calibration...")
                self.obj_points = []
                self.img_points = []
                captured_count = 0
        
        cv2.destroyAllWindows()
        return captured_count >= 5

    def calibrate(self):
        """Perform camera calibration."""
        print("\n" + "="*60)
        print("COMPUTING CALIBRATION")
        print("="*60)
        
        if len(self.obj_points) < 5:
            print("Error: Not enough calibration images!")
            return False
        
        print(f"\nCalibrating with {len(self.obj_points)} images...")
        print("This may take a moment...")
        
        # Perform calibration
        ret, self.camera_matrix, self.dist_coeffs, self.rvecs, self.tvecs = cv2.calibrateCamera(
            self.obj_points, self.img_points, self.image_size, None, None
        )
        
        if not ret:
            print("Error: Calibration failed!")
            return False
        
        # Calculate reprojection error
        total_error = 0
        for i in range(len(self.obj_points)):
            img_points_proj, _ = cv2.projectPoints(
                self.obj_points[i], self.rvecs[i], self.tvecs[i],
                self.camera_matrix, self.dist_coeffs
            )
            error = cv2.norm(self.img_points[i], img_points_proj, cv2.NORM_L2) / len(img_points_proj)
            total_error += error
        
        mean_error = total_error / len(self.obj_points)
        
        print("\nCalibration Results:")
        print("-" * 40)
        print(f"Reprojection Error: {mean_error:.4f} pixels")
        print(f"  (Good if < 0.5, Acceptable if < 1.0)")
        print("\nCamera Matrix:")
        print(self.camera_matrix)
        print("\nDistortion Coefficients:")
        print(self.dist_coeffs.flatten())
        
        return True

    def save_orbslam3_config(self, output_file="phone_camera.yaml"):
        """Save calibration in ORB-SLAM3 format."""
        if self.camera_matrix is None:
            print("Error: No calibration data!")
            return False
        
        # Extract parameters
        fx = float(self.camera_matrix[0, 0])
        fy = float(self.camera_matrix[1, 1])
        cx = float(self.camera_matrix[0, 2])
        cy = float(self.camera_matrix[1, 2])
        
        k1 = float(self.dist_coeffs[0, 0])
        k2 = float(self.dist_coeffs[0, 1])
        p1 = float(self.dist_coeffs[0, 2])
        p2 = float(self.dist_coeffs[0, 3])
        k3 = float(self.dist_coeffs[0, 4]) if self.dist_coeffs.shape[1] > 4 else 0.0
        
        width = self.image_size[0]
        height = self.image_size[1]
        
        # Create ORB-SLAM3 configuration
        config = f"""%YAML:1.0

#--------------------------------------------------------------------------------------------
# Camera Parameters (Phone Camera - Calibrated {datetime.now().strftime('%Y-%m-%d %H:%M')})
#--------------------------------------------------------------------------------------------

# Camera type: Pinhole
Camera.type: "PinHole"

# Camera calibration parameters
Camera.fx: {fx:.6f}
Camera.fy: {fy:.6f}
Camera.cx: {cx:.6f}
Camera.cy: {cy:.6f}

# Distortion parameters (OpenCV model)
Camera.k1: {k1:.6f}
Camera.k2: {k2:.6f}
Camera.p1: {p1:.6f}
Camera.p2: {p2:.6f}
Camera.k3: {k3:.6f}

# Camera resolution
Camera.width: {width}
Camera.height: {height}

# Camera frames per second
Camera.fps: 30

# Color order: RGB or BGR (IP Webcam usually sends BGR)
Camera.RGB: 0

#--------------------------------------------------------------------------------------------
# ORB Parameters
#--------------------------------------------------------------------------------------------

# Number of features per image
ORBextractor.nFeatures: 1500

# Scale factor between levels in the scale pyramid
ORBextractor.scaleFactor: 1.2

# Number of levels in the scale pyramid
ORBextractor.nLevels: 8

# Fast threshold
ORBextractor.iniThFAST: 20
ORBextractor.minThFAST: 7

#--------------------------------------------------------------------------------------------
# Viewer Parameters
#--------------------------------------------------------------------------------------------

Viewer.KeyFrameSize: 0.05
Viewer.KeyFrameLineWidth: 1
Viewer.GraphLineWidth: 0.9
Viewer.PointSize: 2
Viewer.CameraSize: 0.08
Viewer.CameraLineWidth: 3
Viewer.ViewpointX: 0
Viewer.ViewpointY: -0.7
Viewer.ViewpointZ: -1.8
Viewer.ViewpointF: 500
"""
        
        with open(output_file, 'w') as f:
            f.write(config)
        
        print(f"\nSaved ORB-SLAM3 configuration to: {output_file}")
        return True

    def cleanup(self):
        """Release resources."""
        if hasattr(self, 'cap') and self.cap is not None:
            self.cap.release()
        cv2.destroyAllWindows()


def main():
    parser = argparse.ArgumentParser(
        description='Camera calibration tool for ORB-SLAM3 phone demo',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --url http://192.168.1.100:8080/video
  %(prog)s --url http://192.168.1.100:8080/video --rows 9 --cols 6
  %(prog)s --url http://192.168.1.100:8080/video --output my_phone.yaml

Checkerboard Pattern:
  You need a printed checkerboard pattern. The rows and cols parameters
  refer to the number of INNER corners, not squares.
  
  A standard 10x7 checkerboard has 9x6 inner corners.
  
  You can print one from:
  https://docs.opencv.org/4.x/pattern.png
        """
    )
    
    parser.add_argument('--url', type=str, required=True,
                       help='IP Webcam video stream URL (e.g., http://192.168.1.100:8080/video)')
    parser.add_argument('--rows', type=int, default=9,
                       help='Number of inner corners in checkerboard rows (default: 9)')
    parser.add_argument('--cols', type=int, default=6,
                       help='Number of inner corners in checkerboard columns (default: 6)')
    parser.add_argument('--square-size', type=float, default=0.025,
                       help='Size of checkerboard square in meters (default: 0.025 = 2.5cm)')
    parser.add_argument('--num-images', type=int, default=20,
                       help='Number of calibration images to capture (default: 20)')
    parser.add_argument('--output', type=str, default='phone_camera.yaml',
                       help='Output configuration file (default: phone_camera.yaml)')
    
    args = parser.parse_args()
    
    print("\n" + "="*60)
    print("ORB-SLAM3 Phone Demo - Camera Calibration")
    print("="*60)
    
    calibrator = CameraCalibrator(
        stream_url=args.url,
        checkerboard_rows=args.rows,
        checkerboard_cols=args.cols,
        square_size=args.square_size,
        num_images=args.num_images
    )
    
    try:
        # Connect to stream
        if not calibrator.connect_to_stream():
            sys.exit(1)
        
        # Capture calibration images
        if not calibrator.capture_calibration_images():
            print("\nCalibration cancelled or failed!")
            sys.exit(1)
        
        # Perform calibration
        if not calibrator.calibrate():
            sys.exit(1)
        
        # Save configuration
        calibrator.save_orbslam3_config(args.output)
        
        print("\n" + "="*60)
        print("CALIBRATION COMPLETE!")
        print("="*60)
        print(f"\nConfiguration saved to: {args.output}")
        print("\nNext steps:")
        print("  1. Copy the configuration file to your ORB-SLAM3 directory")
        print("  2. Run the phone demo with: ./run_demo.sh")
        print("")
        
    except KeyboardInterrupt:
        print("\n\nCalibration interrupted by user.")
    finally:
        calibrator.cleanup()


if __name__ == "__main__":
    main()

```

```yaml
%YAML:1.0

#--------------------------------------------------------------------------------------------
# Camera Parameters (Phone Camera - Template)
# 
# NOTE: This is a TEMPLATE file with approximate values.
# For accurate SLAM results, run calibrate_camera.py to generate
# proper calibration values for YOUR specific phone camera.
#--------------------------------------------------------------------------------------------

# Camera type: Pinhole
Camera.type: "PinHole"

# Camera calibration parameters (approximate values for 1080p phone camera)
# These MUST be calibrated for your specific phone!
Camera.fx: 1000.0
Camera.fy: 1000.0
Camera.cx: 540.0
Camera.cy: 960.0

# Distortion parameters (OpenCV model)
# Run calibration to get accurate values
Camera.k1: 0.0
Camera.k2: 0.0
Camera.p1: 0.0
Camera.p2: 0.0
Camera.k3: 0.0

# Camera resolution (adjust to match your IP Webcam settings)
Camera.width: 1080
Camera.height: 1920

# Camera frames per second
Camera.fps: 30

# Color order: RGB or BGR (IP Webcam sends BGR by default)
Camera.RGB: 0

#--------------------------------------------------------------------------------------------
# ORB Parameters
#--------------------------------------------------------------------------------------------

# Number of features per image
# Higher = more features but slower
# Lower = faster but may lose tracking
ORBextractor.nFeatures: 1500

# Scale factor between levels in the scale pyramid
ORBextractor.scaleFactor: 1.2

# Number of levels in the scale pyramid
ORBextractor.nLevels: 8

# Fast threshold
# Initial threshold for FAST corner detection
ORBextractor.iniThFAST: 20
# Minimum threshold (used if too few features found)
ORBextractor.minThFAST: 7

#--------------------------------------------------------------------------------------------
# Viewer Parameters
#--------------------------------------------------------------------------------------------

Viewer.KeyFrameSize: 0.05
Viewer.KeyFrameLineWidth: 1
Viewer.GraphLineWidth: 0.9
Viewer.PointSize: 2
Viewer.CameraSize: 0.08
Viewer.CameraLineWidth: 3
Viewer.ViewpointX: 0
Viewer.ViewpointY: -0.7
Viewer.ViewpointZ: -1.8
Viewer.ViewpointF: 500

```

```cpp
/**
 * ORB-SLAM3 Phone Demo - Monocular Phone Camera Interface
 * 
 * This program reads video frames from an IP Webcam stream (Android phone)
 * and processes them with ORB-SLAM3 monocular SLAM.
 * 
 * Usage:
 *   ./phone_mono <path_to_vocabulary> <path_to_settings> <stream_url>
 * 
 * Example:
 *   ./phone_mono ../Vocabulary/ORBvoc.txt phone_camera.yaml http://192.168.1.100:8080/video
 */

#include <iostream>
#include <algorithm>
#include <fstream>
#include <chrono>
#include <iomanip>
#include <csignal>

#include <opencv2/core.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>

#include "System.h"

using namespace std;

// Global flag for graceful shutdown
volatile sig_atomic_t shutdown_requested = 0;

void signal_handler(int signal) {
    cout << "\n[INFO] Shutdown requested (signal " << signal << ")" << endl;
    shutdown_requested = 1;
}

void print_usage(const char* program_name) {
    cout << endl;
    cout << "ORB-SLAM3 Phone Demo - Monocular Mode" << endl;
    cout << "======================================" << endl;
    cout << endl;
    cout << "Usage: " << program_name << " <vocabulary> <settings> <stream_url>" << endl;
    cout << endl;
    cout << "Arguments:" << endl;
    cout << "  vocabulary   Path to ORB vocabulary file (ORBvoc.txt)" << endl;
    cout << "  settings     Path to camera settings YAML file" << endl;
    cout << "  stream_url   IP Webcam video stream URL" << endl;
    cout << endl;
    cout << "Example:" << endl;
    cout << "  " << program_name << " ../Vocabulary/ORBvoc.txt phone_camera.yaml http://192.168.1.100:8080/video" << endl;
    cout << endl;
    cout << "Controls:" << endl;
    cout << "  ESC or 'q'  - Quit the application" << endl;
    cout << "  SPACE       - Pause/resume tracking" << endl;
    cout << "  's'         - Save current map" << endl;
    cout << "  'r'         - Reset SLAM system" << endl;
    cout << endl;
}

class FrameGrabber {
public:
    FrameGrabber(const string& stream_url) : url_(stream_url), connected_(false) {
        connect();
    }

    bool connect() {
        cout << "[INFO] Connecting to stream: " << url_ << endl;
        
        // Try to open the video stream
        cap_.open(url_, cv::CAP_FFMPEG);
        
        if (!cap_.isOpened()) {
            // Try with different backend
            cap_.open(url_, cv::CAP_ANY);
        }
        
        if (!cap_.isOpened()) {
            cerr << "[ERROR] Could not connect to video stream!" << endl;
            cerr << "[ERROR] Make sure IP Webcam is running on your phone." << endl;
            connected_ = false;
            return false;
        }
        
        // Get stream properties
        width_ = static_cast<int>(cap_.get(cv::CAP_PROP_FRAME_WIDTH));
        height_ = static_cast<int>(cap_.get(cv::CAP_PROP_FRAME_HEIGHT));
        fps_ = cap_.get(cv::CAP_PROP_FPS);
        
        if (fps_ <= 0) fps_ = 30.0;  // Default to 30 FPS if not available
        
        cout << "[INFO] Connected! Resolution: " << width_ << "x" << height_ 
             << " @ " << fps_ << " FPS" << endl;
        
        connected_ = true;
        return true;
    }

    bool grab(cv::Mat& frame) {
        if (!connected_) {
            if (!connect()) return false;
        }
        
        if (!cap_.read(frame)) {
            cerr << "[WARN] Failed to grab frame, attempting reconnect..." << endl;
            connected_ = false;
            return false;
        }
        
        return true;
    }

    bool isConnected() const { return connected_; }
    int width() const { return width_; }
    int height() const { return height_; }
    double fps() const { return fps_; }

private:
    string url_;
    cv::VideoCapture cap_;
    bool connected_;
    int width_, height_;
    double fps_;
};

int main(int argc, char **argv) {
    // Check arguments
    if (argc != 4) {
        print_usage(argv[0]);
        return 1;
    }

    string vocabulary_path = argv[1];
    string settings_path = argv[2];
    string stream_url = argv[3];

    // Check if files exist
    ifstream vocab_file(vocabulary_path);
    if (!vocab_file.good()) {
        cerr << "[ERROR] Vocabulary file not found: " << vocabulary_path << endl;
        return 1;
    }
    vocab_file.close();

    ifstream settings_file(settings_path);
    if (!settings_file.good()) {
        cerr << "[ERROR] Settings file not found: " << settings_path << endl;
        return 1;
    }
    settings_file.close();

    // Set up signal handlers for graceful shutdown
    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);

    cout << endl;
    cout << "========================================" << endl;
    cout << "ORB-SLAM3 Phone Demo - Starting..." << endl;
    cout << "========================================" << endl;
    cout << endl;

    // Connect to phone camera stream
    FrameGrabber grabber(stream_url);
    if (!grabber.isConnected()) {
        return 1;
    }

    // Create SLAM system (Monocular mode, with viewer)
    cout << endl << "[INFO] Initializing ORB-SLAM3..." << endl;
    ORB_SLAM3::System SLAM(vocabulary_path, settings_path, ORB_SLAM3::System::MONOCULAR, true);

    cout << endl;
    cout << "[INFO] ORB-SLAM3 initialized successfully!" << endl;
    cout << "[INFO] Starting tracking loop..." << endl;
    cout << endl;
    cout << "Controls: ESC/q=quit, SPACE=pause, s=save, r=reset" << endl;
    cout << endl;

    // Tracking variables
    cv::Mat frame;
    bool paused = false;
    int frame_count = 0;
    int tracking_lost_count = 0;
    
    auto start_time = chrono::steady_clock::now();
    auto last_fps_time = start_time;
    int fps_frame_count = 0;
    double current_fps = 0;

    // Main tracking loop
    while (!shutdown_requested) {
        // Grab frame from phone
        if (!grabber.grab(frame)) {
            // Try to reconnect
            this_thread::sleep_for(chrono::milliseconds(1000));
            continue;
        }

        if (frame.empty()) continue;

        // Calculate timestamp in seconds
        auto now = chrono::steady_clock::now();
        double timestamp = chrono::duration<double>(now - start_time).count();

        // Track frame if not paused
        if (!paused) {
            // Pass frame to SLAM system
            Sophus::SE3f pose = SLAM.TrackMonocular(frame, timestamp);

            // Check tracking state
            int state = SLAM.GetTrackingState();
            
            if (state == 2) {  // OK
                tracking_lost_count = 0;
            } else if (state == 3) {  // LOST
                tracking_lost_count++;
                if (tracking_lost_count % 30 == 1) {
                    cout << "[WARN] Tracking lost! Move camera slowly or return to mapped area." << endl;
                }
            }
        }

        // Calculate FPS
        fps_frame_count++;
        auto fps_elapsed = chrono::duration<double>(now - last_fps_time).count();
        if (fps_elapsed >= 1.0) {
            current_fps = fps_frame_count / fps_elapsed;
            fps_frame_count = 0;
            last_fps_time = now;
        }

        // Display frame with overlay
        cv::Mat display;
        frame.copyTo(display);

        // Draw FPS and status
        string fps_text = "FPS: " + to_string(static_cast<int>(current_fps));
        cv::putText(display, fps_text, cv::Point(10, 30), 
                   cv::FONT_HERSHEY_SIMPLEX, 0.8, cv::Scalar(0, 255, 0), 2);

        int state = SLAM.GetTrackingState();
        string state_text;
        cv::Scalar state_color;
        
        switch (state) {
            case 0:
                state_text = "SYSTEM NOT READY";
                state_color = cv::Scalar(0, 165, 255);  // Orange
                break;
            case 1:
                state_text = "NO IMAGES YET";
                state_color = cv::Scalar(0, 165, 255);  // Orange
                break;
            case 2:
                state_text = "TRACKING OK";
                state_color = cv::Scalar(0, 255, 0);    // Green
                break;
            case 3:
                state_text = "TRACKING LOST";
                state_color = cv::Scalar(0, 0, 255);    // Red
                break;
            default:
                state_text = "UNKNOWN STATE";
                state_color = cv::Scalar(128, 128, 128);
                break;
        }

        if (paused) {
            state_text = "PAUSED";
            state_color = cv::Scalar(0, 255, 255);  // Yellow
        }

        cv::putText(display, state_text, cv::Point(10, 60),
                   cv::FONT_HERSHEY_SIMPLEX, 0.8, state_color, 2);

        // Show keypoints count
        int num_matches = SLAM.GetTrackedMapPoints().size();
        string matches_text = "Matches: " + to_string(num_matches);
        cv::putText(display, matches_text, cv::Point(10, 90),
                   cv::FONT_HERSHEY_SIMPLEX, 0.6, cv::Scalar(255, 255, 255), 2);

        // Display
        cv::imshow("ORB-SLAM3 Phone Demo", display);

        // Handle keyboard input
        int key = cv::waitKey(1);
        
        if (key == 27 || key == 'q' || key == 'Q') {  // ESC or 'q'
            cout << "[INFO] Quit requested" << endl;
            break;
        } else if (key == ' ') {  // Space
            paused = !paused;
            cout << "[INFO] " << (paused ? "Paused" : "Resumed") << endl;
        } else if (key == 's' || key == 'S') {  // Save
            string map_file = "phone_slam_map.osa";
            cout << "[INFO] Saving map to " << map_file << "..." << endl;
            SLAM.SaveAtlas(ORB_SLAM3::System::FILE_TYPE::BINARY_FILE, map_file);
            cout << "[INFO] Map saved!" << endl;
        } else if (key == 'r' || key == 'R') {  // Reset
            cout << "[INFO] Resetting SLAM system..." << endl;
            SLAM.Reset();
            cout << "[INFO] Reset complete" << endl;
        }

        frame_count++;
    }

    // Cleanup
    cout << endl;
    cout << "[INFO] Shutting down..." << endl;
    
    cv::destroyAllWindows();
    
    // Stop SLAM and save trajectory
    cout << "[INFO] Saving trajectory..." << endl;
    SLAM.Shutdown();
    
    // Save trajectory files
    SLAM.SaveTrajectoryTUM("phone_trajectory_tum.txt");
    SLAM.SaveKeyFrameTrajectoryTUM("phone_keyframes_tum.txt");
    
    cout << "[INFO] Trajectory saved to phone_trajectory_tum.txt" << endl;
    cout << endl;
    cout << "========================================" << endl;
    cout << "ORB-SLAM3 Phone Demo - Finished" << endl;
    cout << "========================================" << endl;
    cout << "Processed " << frame_count << " frames" << endl;
    cout << endl;

    return 0;
}

```

```plaintext
cmake_minimum_required(VERSION 3.10)
project(PhoneSlam)

# C++ Standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_BUILD_TYPE Release)

# Compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -march=native")

# ORB-SLAM3 path - adjust this to your installation
# Default assumes ORB_SLAM3 is in home directory
set(ORB_SLAM3_PATH "$ENV{HOME}/orbslam3_phone_demo/ORB_SLAM3" CACHE PATH "Path to ORB-SLAM3")

message(STATUS "ORB-SLAM3 path: ${ORB_SLAM3_PATH}")

# Check if ORB-SLAM3 exists
if(NOT EXISTS "${ORB_SLAM3_PATH}/include/System.h")
    message(FATAL_ERROR 
        "ORB-SLAM3 not found at ${ORB_SLAM3_PATH}\n"
        "Please set ORB_SLAM3_PATH to your ORB-SLAM3 installation directory:\n"
        "  cmake -DORB_SLAM3_PATH=/path/to/ORB_SLAM3 ..")
endif()

# Find required packages
find_package(OpenCV 4.0 REQUIRED)
find_package(Eigen3 3.1.0 REQUIRED)
find_package(Pangolin REQUIRED)

message(STATUS "OpenCV version: ${OpenCV_VERSION}")
message(STATUS "Eigen3 version: ${EIGEN3_VERSION_STRING}")
message(STATUS "Pangolin found: ${Pangolin_FOUND}")

# Include directories
include_directories(
    ${ORB_SLAM3_PATH}
    ${ORB_SLAM3_PATH}/include
    ${ORB_SLAM3_PATH}/include/CameraModels
    ${ORB_SLAM3_PATH}/Thirdparty/Sophus
    ${EIGEN3_INCLUDE_DIR}
    ${Pangolin_INCLUDE_DIRS}
    ${OpenCV_INCLUDE_DIRS}
)

# Link directories
link_directories(
    ${ORB_SLAM3_PATH}/lib
    ${ORB_SLAM3_PATH}/Thirdparty/DBoW2/lib
    ${ORB_SLAM3_PATH}/Thirdparty/g2o/lib
)

# ORB-SLAM3 libraries
set(ORB_SLAM3_LIBS
    ${ORB_SLAM3_PATH}/lib/libORB_SLAM3.so
    ${ORB_SLAM3_PATH}/Thirdparty/DBoW2/lib/libDBoW2.so
    ${ORB_SLAM3_PATH}/Thirdparty/g2o/lib/libg2o.so
)

# Build phone_mono executable
add_executable(phone_mono phone_mono.cc)

target_link_libraries(phone_mono
    ${ORB_SLAM3_LIBS}
    ${OpenCV_LIBS}
    ${EIGEN3_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lboost_serialization
    -lcrypto
)

# Copy necessary files to build directory
configure_file(${CMAKE_SOURCE_DIR}/phone_camera.yaml ${CMAKE_BINARY_DIR}/phone_camera.yaml COPYONLY)

# Create symlink to vocabulary
add_custom_command(TARGET phone_mono POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E create_symlink
        ${ORB_SLAM3_PATH}/Vocabulary
        ${CMAKE_BINARY_DIR}/Vocabulary
    COMMENT "Creating symlink to vocabulary"
)

# Installation
install(TARGETS phone_mono DESTINATION bin)
install(FILES phone_camera.yaml DESTINATION share/phone_slam)

# Print build summary
message(STATUS "")
message(STATUS "=======================================")
message(STATUS "Phone SLAM Build Configuration")
message(STATUS "=======================================")
message(STATUS "ORB-SLAM3 path:  ${ORB_SLAM3_PATH}")
message(STATUS "OpenCV:          ${OpenCV_VERSION}")
message(STATUS "Build type:      ${CMAKE_BUILD_TYPE}")
message(STATUS "=======================================")
message(STATUS "")

```

```shellscript
#!/bin/bash
#
# ORB-SLAM3 Phone Demo - Build Script
# This script builds the phone_mono executable
#

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
ORB_SLAM3_PATH="${ORB_SLAM3_PATH:-$HOME/orbslam3_phone_demo/ORB_SLAM3}"

echo "=========================================="
echo "ORB-SLAM3 Phone Demo - Building"
echo "=========================================="
echo ""

# Check if ORB-SLAM3 exists
if [ ! -d "$ORB_SLAM3_PATH" ]; then
    print_error "ORB-SLAM3 not found at: $ORB_SLAM3_PATH"
    echo ""
    echo "Please run build_deps.sh first, or set ORB_SLAM3_PATH:"
    echo "  export ORB_SLAM3_PATH=/path/to/ORB_SLAM3"
    echo "  ./build_phone_demo.sh"
    exit 1
fi

print_status "Using ORB-SLAM3 at: $ORB_SLAM3_PATH"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Create build directory
print_status "Creating build directory..."
mkdir -p build
cd build

# Run CMake
print_status "Running CMake..."
cmake .. -DORB_SLAM3_PATH="$ORB_SLAM3_PATH" -DCMAKE_BUILD_TYPE=Release

# Build
print_status "Building phone_mono..."
make -j$(nproc)

echo ""
echo "=========================================="
echo -e "${GREEN}Build Complete!${NC}"
echo "=========================================="
echo ""
echo "Executable: $SCRIPT_DIR/build/phone_mono"
echo ""
echo "Usage:"
echo "  ./phone_mono Vocabulary/ORBvoc.txt phone_camera.yaml <stream_url>"
echo ""
echo "Example:"
echo "  ./phone_mono Vocabulary/ORBvoc.txt phone_camera.yaml http://192.168.1.100:8080/video"
echo ""

```

```shellscript
#!/bin/bash
#
# ORB-SLAM3 Phone Demo - Run Script
# This script launches the ORB-SLAM3 phone demo
#

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}$1${NC}"
}

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Configuration
BUILD_DIR="$SCRIPT_DIR/build"
VOCABULARY="$BUILD_DIR/Vocabulary/ORBvoc.txt"
SETTINGS="$BUILD_DIR/phone_camera.yaml"
PHONE_MONO="$BUILD_DIR/phone_mono"

# Default stream URL (can be overridden by argument)
STREAM_URL="${1:-}"

echo ""
print_header "=========================================="
print_header "ORB-SLAM3 Phone Demo"
print_header "=========================================="
echo ""

# Check if demo is built
if [ ! -f "$PHONE_MONO" ]; then
    print_error "phone_mono not found!"
    echo ""
    echo "Please build the demo first:"
    echo "  ./build_phone_demo.sh"
    exit 1
fi

# Check vocabulary
if [ ! -f "$VOCABULARY" ]; then
    print_error "Vocabulary file not found: $VOCABULARY"
    echo ""
    echo "Please ensure ORB-SLAM3 was built correctly."
    exit 1
fi

# Check settings
if [ ! -f "$SETTINGS" ]; then
    print_warning "Settings file not found in build dir, copying..."
    cp "$SCRIPT_DIR/phone_camera.yaml" "$BUILD_DIR/" 2>/dev/null || true
fi

# Get stream URL if not provided
if [ -z "$STREAM_URL" ]; then
    echo ""
    echo "Please enter your phone's IP Webcam stream URL."
    echo ""
    echo "To find your URL:"
    echo "  1. Open IP Webcam app on your Android phone"
    echo "  2. Scroll to bottom and tap 'Start server'"
    echo "  3. Note the IP address shown (e.g., 192.168.1.100:8080)"
    echo ""
    read -p "Enter stream URL (e.g., http://192.168.1.100:8080/video): " STREAM_URL
    echo ""
fi

# Validate URL format
if [[ ! "$STREAM_URL" =~ ^https?:// ]]; then
    # Add http:// if not present
    STREAM_URL="http://$STREAM_URL"
fi

# Add /video if not present
if [[ ! "$STREAM_URL" =~ /video$ ]] && [[ ! "$STREAM_URL" =~ /videofeed$ ]]; then
    STREAM_URL="${STREAM_URL}/video"
fi

print_status "Stream URL: $STREAM_URL"
echo ""

# Test connection first
print_status "Testing connection to phone..."
if command -v curl &> /dev/null; then
    # Try to connect with timeout
    if ! curl -s --connect-timeout 5 "${STREAM_URL%/video}/" > /dev/null 2>&1; then
        print_warning "Could not reach phone at this address."
        echo ""
        echo "Troubleshooting:"
        echo "  1. Make sure IP Webcam app is running"
        echo "  2. Check that phone and PC are on the same network"
        echo "  3. Verify the IP address is correct"
        echo ""
        read -p "Try to continue anyway? (y/N): " continue_anyway
        if [[ ! "$continue_anyway" =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        print_status "Connection successful!"
    fi
fi

echo ""
print_header "=========================================="
print_header "Starting ORB-SLAM3"
print_header "=========================================="
echo ""
echo "Controls:"
echo "  ESC or 'q'  - Quit"
echo "  SPACE       - Pause/Resume"
echo "  's'         - Save map"
echo "  'r'         - Reset SLAM"
echo ""
echo "Tips for good tracking:"
echo "  - Move the camera slowly and smoothly"
echo "  - Point at textured surfaces (walls, furniture)"
echo "  - Avoid pointing at blank walls or sky"
echo "  - Keep good lighting"
echo ""
print_status "Launching..."
echo ""

# Set library path
export LD_LIBRARY_PATH="$HOME/orbslam3_phone_demo/ORB_SLAM3/lib:$HOME/orbslam3_phone_demo/ORB_SLAM3/Thirdparty/DBoW2/lib:$HOME/orbslam3_phone_demo/ORB_SLAM3/Thirdparty/g2o/lib:$LD_LIBRARY_PATH"

# Run the demo
cd "$BUILD_DIR"
./phone_mono "$VOCABULARY" phone_camera.yaml "$STREAM_URL"

echo ""
print_status "Demo finished."
echo ""

```

```shellscript
#!/bin/bash
#
# ORB-SLAM3 Phone Demo - Stream Test Script
# This script tests the connection to your phone's camera stream
#

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo ""
echo "=========================================="
echo "ORB-SLAM3 Phone Demo - Stream Test"
echo "=========================================="
echo ""

# Get stream URL
STREAM_URL="${1:-}"

if [ -z "$STREAM_URL" ]; then
    echo "Usage: $0 <stream_url>"
    echo ""
    echo "Example:"
    echo "  $0 http://192.168.1.100:8080/video"
    echo ""
    echo "Setup instructions:"
    echo "  1. Install 'IP Webcam' app from Google Play Store"
    echo "  2. Open the app on your Android phone"
    echo "  3. Scroll to bottom and tap 'Start server'"
    echo "  4. Note the IP address shown (e.g., 192.168.1.100:8080)"
    echo "  5. Run this script with: $0 http://<ip>:<port>/video"
    echo ""
    exit 1
fi

# Validate URL
if [[ ! "$STREAM_URL" =~ ^https?:// ]]; then
    STREAM_URL="http://$STREAM_URL"
fi

BASE_URL="${STREAM_URL%/video}"
BASE_URL="${BASE_URL%/}"

print_status "Testing connection to: $BASE_URL"
echo ""

# Test 1: Basic connectivity
echo "Test 1: Basic connectivity..."
if command -v curl &> /dev/null; then
    if curl -s --connect-timeout 5 "$BASE_URL/" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓ Server is reachable${NC}"
    else
        echo -e "  ${RED}✗ Cannot reach server${NC}"
        echo ""
        print_error "Could not connect to $BASE_URL"
        echo ""
        echo "Please check:"
        echo "  1. IP Webcam is running on your phone"
        echo "  2. Phone and PC are on the same network"
        echo "  3. IP address and port are correct"
        echo "  4. No firewall is blocking the connection"
        exit 1
    fi
else
    print_warning "curl not installed, skipping connectivity test"
fi

# Test 2: Video stream
echo ""
echo "Test 2: Video stream..."
if command -v ffprobe &> /dev/null; then
    STREAM_INFO=$(ffprobe -v quiet -show_entries stream=width,height,r_frame_rate -of csv=p=0 "$STREAM_URL" 2>&1 | head -1)
    if [ -n "$STREAM_INFO" ]; then
        IFS=',' read -r WIDTH HEIGHT FPS <<< "$STREAM_INFO"
        echo -e "  ${GREEN}✓ Video stream accessible${NC}"
        echo "    Resolution: ${WIDTH}x${HEIGHT}"
        echo "    Frame rate: $FPS"
    else
        print_warning "Could not get stream info (stream might still work)"
    fi
else
    print_warning "ffprobe not installed, skipping stream info"
fi

# Test 3: Open video with ffplay
echo ""
echo "Test 3: Visual test..."
if command -v ffplay &> /dev/null; then
    echo "Opening video stream in ffplay..."
    echo "Press 'q' to quit the video window"
    echo ""
    ffplay -loglevel warning -window_title "IP Webcam Test" "$STREAM_URL" &
    FFPLAY_PID=$!
    
    sleep 3
    
    if kill -0 $FFPLAY_PID 2>/dev/null; then
        echo -e "${GREEN}✓ Video stream is working!${NC}"
        echo ""
        echo "Press Enter to close the test window and continue..."
        read
        kill $FFPLAY_PID 2>/dev/null
    else
        echo -e "${RED}✗ ffplay exited unexpectedly${NC}"
    fi
elif command -v cvlc &> /dev/null; then
    echo "Opening video stream in VLC..."
    cvlc --play-and-exit "$STREAM_URL" &
    sleep 5
else
    print_warning "No video player found (ffplay or vlc)"
    echo "Install ffmpeg: sudo apt install ffmpeg"
fi

echo ""
echo "=========================================="
echo "Stream Test Complete"
echo "=========================================="
echo ""
echo "Your stream URL: $STREAM_URL"
echo ""
echo "If the video displayed correctly, you can now:"
echo "  1. Run calibration: python3 calibrate_camera.py --url $STREAM_URL"
echo "  2. Run the demo: ./run_demo.sh $STREAM_URL"
echo ""

```

```markdown
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

```

All files have been created. Here's a summary of the complete ORB-SLAM3 phone demo setup:

## Files Created

| File | Purpose |
|------|---------|
| `setup_wsl.sh` | Installs all WSL2 dependencies (build tools, OpenCV, Pangolin libs) |
| `build_deps.sh` | Builds Pangolin, OpenCV 4.x, and ORB-SLAM3 from source |
| `calibrate_camera.py` | Interactive camera calibration using checkerboard pattern |
| `phone_camera.yaml` | Camera settings template for ORB-SLAM3 |
| `phone_mono.cc` | C++ interface that reads IP Webcam stream and feeds ORB-SLAM3 |
| `CMakeLists.txt` | Build configuration for the phone demo |
| `build_phone_demo.sh` | Builds the `phone_mono` executable |
| `run_demo.sh` | Launches the demo with proper environment setup |
| `test_stream.sh` | Tests connectivity to your phone's camera stream |
| `README.md` | Complete documentation with troubleshooting |

## How to Use (In Order)

1. **Copy files to WSL2:**
   ```bash
   # From Windows, copy to WSL2 home directory
   cp -r /mnt/c/Users/emirh/Desktop/demotestr ~/orbslam3-phone-demo
   cd ~/orbslam3-phone-demo
   ```

2. **Install dependencies:** `./setup_wsl.sh` (~15 min)

3. **Build everything:** `./build_deps.sh` (~30-60 min)

4. **Install IP Webcam** on your Android phone (Google Play, free)

5. **Test the stream:** `./test_stream.sh http://<phone-ip>:8080/video`

6. **Calibrate camera:** `python3 calibrate_camera.py --url http://<phone-ip>:8080/video`

7. **Build the demo:** `./build_phone_demo.sh`

8. **Run it:** `./run_demo.sh http://<phone-ip>:8080/video`

The demo will show a live camera feed with feature tracking and a 3D map visualization window!

```shellscript
   # From Windows, copy to WSL2 home directory
   cp -r /mnt/c/Users/emirh/Desktop/demotestr ~/orbslam3-phone-demo
   cd ~/orbslam3-phone-demo
```

