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
print_status "Demo finished."ewt
echo ""

