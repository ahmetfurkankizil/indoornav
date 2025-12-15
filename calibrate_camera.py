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

