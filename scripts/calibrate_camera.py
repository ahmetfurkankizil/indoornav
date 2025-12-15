#!/usr/bin/env python3
"""
Camera Calibration Script for ORB-SLAM3

This script helps calibrate your phone camera using a checkerboard pattern.
Print a checkerboard pattern and capture images from different angles.

Usage:
    1. Print a checkerboard pattern (e.g., 9x6 inner corners)
    2. Capture 15-20 images from your phone at different angles
    3. Run: python calibrate_camera.py --images ./calibration_images/ --rows 6 --cols 9
    4. Update the camera intrinsics in your ORB-SLAM3 config file
"""

import cv2
import numpy as np
import glob
import argparse
import os


def calibrate_camera(image_dir: str, rows: int, cols: int, square_size: float = 1.0):
    """
    Calibrate camera using checkerboard images.
    
    Args:
        image_dir: Directory containing calibration images
        rows: Number of inner corners in rows (height)
        cols: Number of inner corners in columns (width)
        square_size: Size of checkerboard square in desired units (e.g., cm)
    
    Returns:
        Camera matrix, distortion coefficients, and calibration error
    """
    # Termination criteria for corner refinement
    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
    
    # Prepare object points
    objp = np.zeros((rows * cols, 3), np.float32)
    objp[:, :2] = np.mgrid[0:cols, 0:rows].T.reshape(-1, 2)
    objp *= square_size
    
    # Arrays to store object points and image points
    obj_points = []  # 3D points in real world
    img_points = []  # 2D points in image plane
    
    # Get all images
    image_paths = glob.glob(os.path.join(image_dir, '*.jpg')) + \
                  glob.glob(os.path.join(image_dir, '*.png')) + \
                  glob.glob(os.path.join(image_dir, '*.jpeg'))
    
    if not image_paths:
        print(f"No images found in {image_dir}")
        return None, None, None
    
    print(f"Found {len(image_paths)} images")
    
    img_size = None
    successful = 0
    
    for i, path in enumerate(image_paths):
        img = cv2.imread(path)
        if img is None:
            print(f"Failed to load: {path}")
            continue
            
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        
        if img_size is None:
            img_size = gray.shape[::-1]
        
        # Find checkerboard corners
        ret, corners = cv2.findChessboardCorners(gray, (cols, rows), None)
        
        if ret:
            # Refine corners
            corners2 = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
            
            obj_points.append(objp)
            img_points.append(corners2)
            successful += 1
            
            # Draw and display corners (optional visualization)
            cv2.drawChessboardCorners(img, (cols, rows), corners2, ret)
            cv2.imshow('Calibration', cv2.resize(img, (640, 480)))
            cv2.waitKey(100)
            
            print(f"[{i+1}/{len(image_paths)}] {os.path.basename(path)}: SUCCESS")
        else:
            print(f"[{i+1}/{len(image_paths)}] {os.path.basename(path)}: FAILED (no corners found)")
    
    cv2.destroyAllWindows()
    
    if successful < 10:
        print(f"\nWarning: Only {successful} successful images. Need at least 10 for good calibration.")
    
    print(f"\nCalibrating with {successful} images...")
    
    # Calibrate camera
    ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
        obj_points, img_points, img_size, None, None
    )
    
    # Calculate reprojection error
    total_error = 0
    for i in range(len(obj_points)):
        img_points2, _ = cv2.projectPoints(obj_points[i], rvecs[i], tvecs[i], 
                                            camera_matrix, dist_coeffs)
        error = cv2.norm(img_points[i], img_points2, cv2.NORM_L2) / len(img_points2)
        total_error += error
    
    mean_error = total_error / len(obj_points)
    
    return camera_matrix, dist_coeffs, mean_error


def print_results(camera_matrix, dist_coeffs, error, output_file: str = None):
    """Print and optionally save calibration results."""
    fx = camera_matrix[0, 0]
    fy = camera_matrix[1, 1]
    cx = camera_matrix[0, 2]
    cy = camera_matrix[1, 2]
    
    k1, k2, p1, p2, k3 = dist_coeffs.flatten()[:5] if len(dist_coeffs.flatten()) >= 5 else \
                         list(dist_coeffs.flatten()) + [0] * (5 - len(dist_coeffs.flatten()))
    
    result = f"""
================================================================================
                        CAMERA CALIBRATION RESULTS
================================================================================

Reprojection Error: {error:.6f} pixels
(Lower is better. < 0.5 is good, < 0.1 is excellent)

Camera Matrix:
  fx: {fx:.6f}
  fy: {fy:.6f}
  cx: {cx:.6f}
  cy: {cy:.6f}

Distortion Coefficients:
  k1: {k1:.6f}
  k2: {k2:.6f}
  p1: {p1:.6f}
  p2: {p2:.6f}
  k3: {k3:.6f}

================================================================================
                    YAML CONFIG (copy to your settings file)
================================================================================

# Camera intrinsic parameters
Camera.fx: {fx:.1f}
Camera.fy: {fy:.1f}
Camera.cx: {cx:.1f}
Camera.cy: {cy:.1f}

# Distortion coefficients
Camera.k1: {k1:.6f}
Camera.k2: {k2:.6f}
Camera.p1: {p1:.6f}
Camera.p2: {p2:.6f}
Camera.k3: {k3:.6f}

================================================================================
"""
    print(result)
    
    if output_file:
        with open(output_file, 'w') as f:
            f.write(result)
        print(f"Results saved to: {output_file}")


def main():
    parser = argparse.ArgumentParser(description='Camera calibration using checkerboard')
    parser.add_argument('--images', '-i', required=True, help='Directory containing calibration images')
    parser.add_argument('--rows', '-r', type=int, default=6, help='Number of inner corners (rows)')
    parser.add_argument('--cols', '-c', type=int, default=9, help='Number of inner corners (columns)')
    parser.add_argument('--square-size', '-s', type=float, default=1.0, help='Square size in desired units')
    parser.add_argument('--output', '-o', help='Output file for results')
    
    args = parser.parse_args()
    
    print("="*60)
    print("Camera Calibration Tool")
    print("="*60)
    print(f"Image directory: {args.images}")
    print(f"Checkerboard: {args.cols}x{args.rows} inner corners")
    print(f"Square size: {args.square_size}")
    print()
    
    camera_matrix, dist_coeffs, error = calibrate_camera(
        args.images, args.rows, args.cols, args.square_size
    )
    
    if camera_matrix is not None:
        print_results(camera_matrix, dist_coeffs, error, args.output)
    else:
        print("Calibration failed!")
        return 1
    
    return 0


if __name__ == '__main__':
    exit(main())

