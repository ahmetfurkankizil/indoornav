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
# Note: libtbb2 was replaced by libtbb12 in Ubuntu 22.04+
sudo apt install -y \
    libeigen3-dev \
    libboost-all-dev \
    libssl-dev \
    libtbb12 \
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

