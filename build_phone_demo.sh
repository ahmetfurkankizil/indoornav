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

