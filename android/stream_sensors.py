#!/usr/bin/env python3
"""
Android Sensor Streaming Script for ORB-SLAM3
Run this script on your Android phone using Termux with Termux:API installed.

Installation on Android:
1. Install Termux from F-Droid (NOT Play Store)
2. Install Termux:API from F-Droid
3. Run: pkg install python termux-api
4. Run: pip install opencv-python numpy

Usage:
    python stream_sensors.py --host <WSL2_IP> --camera-port 5000 --imu-port 5001
"""

import socket
import struct
import time
import json
import threading
import argparse
import subprocess
import sys

# Try to import cv2 for camera, fall back to termux-camera if not available
try:
    import cv2
    HAS_CV2 = True
except ImportError:
    HAS_CV2 = False
    print("OpenCV not available, using termux-camera-photo")

import numpy as np

class IMUStreamer:
    """Streams IMU data (accelerometer + gyroscope) via UDP"""
    
    def __init__(self, host: str, port: int, rate_hz: int = 200):
        self.host = host
        self.port = port
        self.rate_hz = rate_hz
        self.running = False
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        
    def get_sensor_data(self, sensor_type: str) -> dict:
        """Get sensor data using termux-sensor"""
        try:
            result = subprocess.run(
                ['termux-sensor', '-s', sensor_type, '-n', '1'],
                capture_output=True, text=True, timeout=1
            )
            if result.returncode == 0:
                data = json.loads(result.stdout)
                if sensor_type in data:
                    return data[sensor_type]
        except Exception as e:
            pass
        return None
    
    def stream(self):
        """Main streaming loop for IMU data"""
        print(f"Starting IMU stream to {self.host}:{self.port} at {self.rate_hz}Hz")
        self.running = True
        interval = 1.0 / self.rate_hz
        
        while self.running:
            start_time = time.time()
            timestamp = time.time()
            
            # Get accelerometer data
            accel = self.get_sensor_data('accelerometer')
            gyro = self.get_sensor_data('gyroscope')
            
            if accel and gyro:
                # Pack IMU data: timestamp (double) + accel (3 floats) + gyro (3 floats)
                # Total: 8 + 12 + 12 = 32 bytes
                packet = struct.pack(
                    'd3f3f',
                    timestamp,
                    accel['values'][0], accel['values'][1], accel['values'][2],
                    gyro['values'][0], gyro['values'][1], gyro['values'][2]
                )
                try:
                    self.socket.sendto(packet, (self.host, self.port))
                except Exception as e:
                    print(f"IMU send error: {e}")
            
            # Maintain target rate
            elapsed = time.time() - start_time
            if elapsed < interval:
                time.sleep(interval - elapsed)
    
    def stop(self):
        self.running = False
        self.socket.close()


class CameraStreamer:
    """Streams camera frames via TCP"""
    
    def __init__(self, host: str, port: int, fps: int = 30, width: int = 640, height: int = 480):
        self.host = host
        self.port = port
        self.fps = fps
        self.width = width
        self.height = height
        self.running = False
        
    def stream_cv2(self):
        """Stream using OpenCV (if available)"""
        print(f"Starting camera stream to {self.host}:{self.port} at {self.fps}fps using OpenCV")
        
        # Try to open camera
        cap = cv2.VideoCapture(0)
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, self.width)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, self.height)
        cap.set(cv2.CAP_PROP_FPS, self.fps)
        
        if not cap.isOpened():
            print("Failed to open camera with OpenCV")
            return
        
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            sock.connect((self.host, self.port))
        except Exception as e:
            print(f"Failed to connect to receiver: {e}")
            cap.release()
            return
        
        self.running = True
        interval = 1.0 / self.fps
        
        while self.running:
            start_time = time.time()
            timestamp = time.time()
            
            ret, frame = cap.read()
            if ret:
                # Encode frame as JPEG
                _, encoded = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
                data = encoded.tobytes()
                
                # Send: timestamp (8 bytes) + size (4 bytes) + jpeg data
                header = struct.pack('dI', timestamp, len(data))
                try:
                    sock.sendall(header + data)
                except Exception as e:
                    print(f"Camera send error: {e}")
                    break
            
            elapsed = time.time() - start_time
            if elapsed < interval:
                time.sleep(interval - elapsed)
        
        cap.release()
        sock.close()
    
    def stream_termux(self):
        """Stream using termux-camera-photo (fallback)"""
        print(f"Starting camera stream to {self.host}:{self.port} using termux-camera-photo")
        
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            sock.connect((self.host, self.port))
        except Exception as e:
            print(f"Failed to connect to receiver: {e}")
            return
        
        self.running = True
        interval = 1.0 / self.fps
        tmp_file = '/data/data/com.termux/files/home/tmp_frame.jpg'
        
        while self.running:
            start_time = time.time()
            timestamp = time.time()
            
            try:
                # Capture photo using termux
                subprocess.run(
                    ['termux-camera-photo', '-c', '0', tmp_file],
                    capture_output=True, timeout=2
                )
                
                with open(tmp_file, 'rb') as f:
                    data = f.read()
                
                # Send: timestamp (8 bytes) + size (4 bytes) + jpeg data
                header = struct.pack('dI', timestamp, len(data))
                sock.sendall(header + data)
                
            except Exception as e:
                print(f"Camera capture error: {e}")
            
            elapsed = time.time() - start_time
            if elapsed < interval:
                time.sleep(interval - elapsed)
        
        sock.close()
    
    def stream(self):
        if HAS_CV2:
            self.stream_cv2()
        else:
            self.stream_termux()
    
    def stop(self):
        self.running = False


def main():
    parser = argparse.ArgumentParser(description='Stream phone sensors to ORB-SLAM3')
    parser.add_argument('--host', type=str, required=True, help='WSL2 IP address')
    parser.add_argument('--camera-port', type=int, default=5000, help='Camera TCP port')
    parser.add_argument('--imu-port', type=int, default=5001, help='IMU UDP port')
    parser.add_argument('--fps', type=int, default=30, help='Camera FPS')
    parser.add_argument('--imu-rate', type=int, default=200, help='IMU rate in Hz')
    parser.add_argument('--width', type=int, default=640, help='Camera width')
    parser.add_argument('--height', type=int, default=480, help='Camera height')
    
    args = parser.parse_args()
    
    print(f"Streaming to {args.host}")
    print(f"Camera: TCP port {args.camera_port} at {args.fps}fps ({args.width}x{args.height})")
    print(f"IMU: UDP port {args.imu_port} at {args.imu_rate}Hz")
    
    # Create streamers
    imu_streamer = IMUStreamer(args.host, args.imu_port, args.imu_rate)
    camera_streamer = CameraStreamer(args.host, args.camera_port, args.fps, args.width, args.height)
    
    # Start IMU in background thread
    imu_thread = threading.Thread(target=imu_streamer.stream, daemon=True)
    imu_thread.start()
    
    # Run camera in main thread
    try:
        camera_streamer.stream()
    except KeyboardInterrupt:
        print("\nStopping...")
    finally:
        imu_streamer.stop()
        camera_streamer.stop()


if __name__ == '__main__':
    main()

