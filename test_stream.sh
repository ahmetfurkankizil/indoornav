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

