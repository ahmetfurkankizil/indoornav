#!/bin/bash
#
# ORB-SLAM3 Phone Live Demo Runner
# 
# This script helps you run the live ORB-SLAM3 demo with your phone.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ORB_SLAM3_DIR="$PROJECT_DIR/ORB_SLAM3"
PHONE_BRIDGE_DIR="$PROJECT_DIR/phone_bridge"
CONFIG_FILE="$PROJECT_DIR/configs/samsung_s23fe.yaml"
VOCABULARY="$ORB_SLAM3_DIR/Vocabulary/ORBvoc.txt"

# Default ports
CAMERA_PORT=5000
IMU_PORT=5001

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}  ORB-SLAM3 Phone Live Demo${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo ""
}

get_wsl_ip() {
    # Get WSL2 IP address
    ip addr show eth0 2>/dev/null | grep "inet " | awk '{print $2}' | cut -d/ -f1
}

check_requirements() {
    echo -e "${YELLOW}Checking requirements...${NC}"
    
    # Check ORB-SLAM3
    if [ ! -f "$ORB_SLAM3_DIR/lib/libORB_SLAM3.so" ]; then
        echo -e "${RED}ERROR: ORB-SLAM3 not built!${NC}"
        echo "Run: cd $ORB_SLAM3_DIR && ./build.sh"
        exit 1
    fi
    echo -e "${GREEN}✓ ORB-SLAM3 library found${NC}"
    
    # Check vocabulary
    if [ ! -f "$VOCABULARY" ]; then
        echo -e "${RED}ERROR: Vocabulary file not found!${NC}"
        echo "Run: cd $ORB_SLAM3_DIR/Vocabulary && tar -xf ORBvoc.txt.tar.gz"
        exit 1
    fi
    echo -e "${GREEN}✓ Vocabulary file found${NC}"
    
    # Check phone_bridge
    if [ ! -f "$PHONE_BRIDGE_DIR/build/phone_slam" ]; then
        echo -e "${YELLOW}Phone bridge not built. Building now...${NC}"
        build_phone_bridge
    fi
    echo -e "${GREEN}✓ Phone bridge executable found${NC}"
    
    # Check config file
    if [ ! -f "$CONFIG_FILE" ]; then
        echo -e "${RED}ERROR: Config file not found: $CONFIG_FILE${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Config file found${NC}"
    
    echo ""
}

build_phone_bridge() {
    echo -e "${YELLOW}Building phone bridge...${NC}"
    cd "$PHONE_BRIDGE_DIR"
    mkdir -p build
    cd build
    cmake .. -DPangolin_DIR=/home/lkilic/IMU/Pangolin/build
    make -j$(nproc)
    cd "$PROJECT_DIR"
    echo -e "${GREEN}Phone bridge built successfully!${NC}"
}

print_phone_instructions() {
    WSL_IP=$(get_wsl_ip)
    
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}  PHONE SETUP INSTRUCTIONS${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo ""
    echo -e "${YELLOW}1. Install apps on your Android phone:${NC}"
    echo "   - Termux (from F-Droid, NOT Play Store)"
    echo "   - Termux:API (from F-Droid)"
    echo ""
    echo -e "${YELLOW}2. In Termux, run these commands:${NC}"
    echo "   pkg install python termux-api"
    echo "   pip install numpy"
    echo ""
    echo -e "${YELLOW}3. Copy the streaming script to your phone:${NC}"
    echo "   Location: $PROJECT_DIR/android/stream_sensors.py"
    echo ""
    echo -e "${YELLOW}4. Run the streaming script on your phone:${NC}"
    echo -e "   ${GREEN}python stream_sensors.py --host $WSL_IP --camera-port $CAMERA_PORT --imu-port $IMU_PORT${NC}"
    echo ""
    echo -e "${YELLOW}5. Make sure:${NC}"
    echo "   - Phone and PC are on the same WiFi network"
    echo "   - Windows Firewall allows connections to WSL2"
    echo ""
    echo -e "${BLUE}Your WSL2 IP: ${GREEN}$WSL_IP${NC}"
    echo ""
}

run_slam() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}  Starting ORB-SLAM3${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo ""
    
    export LD_LIBRARY_PATH="$ORB_SLAM3_DIR/lib:$ORB_SLAM3_DIR/Thirdparty/DBoW2/lib:$ORB_SLAM3_DIR/Thirdparty/g2o/lib:$LD_LIBRARY_PATH"
    
    echo "Vocabulary: $VOCABULARY"
    echo "Config: $CONFIG_FILE"
    echo "Camera port: $CAMERA_PORT"
    echo "IMU port: $IMU_PORT"
    echo ""
    echo -e "${YELLOW}Waiting for phone connection...${NC}"
    echo "Press Ctrl+C to stop"
    echo ""
    
    "$PHONE_BRIDGE_DIR/build/phone_slam" "$VOCABULARY" "$CONFIG_FILE" "$CAMERA_PORT" "$IMU_PORT"
}

case "${1:-run}" in
    build)
        build_phone_bridge
        ;;
    check)
        print_header
        check_requirements
        echo -e "${GREEN}All requirements satisfied!${NC}"
        ;;
    instructions)
        print_header
        print_phone_instructions
        ;;
    run)
        print_header
        check_requirements
        print_phone_instructions
        echo -e "${YELLOW}Press Enter when phone is ready...${NC}"
        read
        run_slam
        ;;
    *)
        echo "Usage: $0 [build|check|instructions|run]"
        echo ""
        echo "Commands:"
        echo "  build        - Build the phone bridge"
        echo "  check        - Check all requirements"
        echo "  instructions - Show phone setup instructions"
        echo "  run          - Run the full demo (default)"
        exit 1
        ;;
esac

