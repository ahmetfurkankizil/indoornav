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

# Sophus is header-only, no need to build it
# The build step causes issues with newer GCC compilers treating warnings as errors
print_status "Sophus is header-only - skipping build (not needed)"

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

