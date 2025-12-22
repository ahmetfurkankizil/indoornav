import open3d as o3d
import numpy as np
import matplotlib.pyplot as plt
import random

# Load point cloud data
point_cloud = np.loadtxt('ornek_ev_1_lidar_export.xyz', skiprows=1)
point_cloud_o3d = o3d.geometry.PointCloud()
point_cloud_o3d.points = o3d.utility.Vector3dVector(point_cloud[:, :3])

# Clear data
cl, ind = point_cloud_o3d.remove_statistical_outlier(nb_neighbors=20, std_ratio=5.0)
downsampled_point_cloud = cl.voxel_down_sample(voxel_size=0.02)

# Remove ground
plane_model, inliers = downsampled_point_cloud.segment_plane(distance_threshold=0.02, ransac_n=3, num_iterations=1000)
inlier_cloud = downsampled_point_cloud.select_by_index(inliers)
outlier_cloud = downsampled_point_cloud.select_by_index(inliers, invert=True)

# Detect walls and calculate their dimensions
remaining_points = outlier_cloud
detected_walls = []
max_dist_threshold = 0.00007  # Adjust this value based on your needs

for i in range(100):
    if len(remaining_points.points) < 10000:  # If few points are left, stop the loop
        break

    # Segment a plane which is likely to be a wall
    plane_model, inliers = remaining_points.segment_plane(distance_threshold=0.00002, ransac_n=3, num_iterations=10000)
    wall_candidate = remaining_points.select_by_index(inliers)

    # Calculate the dimensions using bounding box
    bounding_box = wall_candidate.get_axis_aligned_bounding_box()
    min_bound = bounding_box.get_min_bound()
    max_bound = bounding_box.get_max_bound()
    length = max_bound[0] - min_bound[0]
    width = max_bound[1] - min_bound[1]
    height = max_bound[2] - min_bound[2]

    # Check dimensions
    if height > 2.0 and width >= 0.0 and length >= 0.0:
        detected_walls.append((wall_candidate, length, width, height))
        print(f"Wall {len(detected_walls)} - Length: {length:.2f} units, Width: {width:.2f} units, Height: {height:.2f} units")

    # Update remaining points
    remaining_points = remaining_points.select_by_index(inliers, invert=True)

# Plot all detected walls in different colors
plt.figure(figsize=(10, 10))
for i, (wall, length, width, height) in enumerate(detected_walls, start=1):
    points_2d = np.asarray(wall.points)[:, :2]
    plt.scatter(points_2d[:, 0], points_2d[:, 1], s=0.1, label=f'Wall {i} L{length:.2f} W{width:.2f} H{height:.2f}')

plt.axis('equal')
plt.xlabel('X')
plt.ylabel('Y')
plt.title('2D Projection of Detected Walls')
plt.legend()
plt.show()

# Simulate WiFi signal propagation considering walls
grid_size = 100
environment = np.zeros((grid_size, grid_size))  # 0 for open space, 1 for walls

# Populate the grid based on detected walls (convert real-world coordinates to grid indices)
scale_factor = 100 / (max_bound[0] - min_bound[0])  # Adjust scale factor based on the area and grid size
for wall, _, _, _ in detected_walls:
    bbox = wall.get_axis_aligned_bounding_box()
    min_bound = bbox.get_min_bound()
    max_bound = bbox.get_max_bound()
    min_x = int(min_bound[0] * scale_factor)
    max_x = int(max_bound[0] * scale_factor)
    min_y = int(min_bound[1] * scale_factor)
    max_y = int(max_bound[1] * scale_factor)
    environment[min_x:max_x, min_y:max_y] = 1

# WiFi propagation function
def wifi_propagation(grid, source_x, source_y, attenuation_factor=0.05, wall_attenuation=20):
    signal = np.zeros_like(grid)
    for ix in range(grid.shape[0]):
        for iy in range(grid.shape[1]):
            path = np.linspace([source_x, source_y], [ix, iy], num=100)
            path_indices = np.round(path).astype(int)
            if np.any(grid[path_indices[:, 0], path_indices[:, 1]] == 1):  # Path through a wall
                signal[ix, iy] = 0  # Or apply wall_attenuation
            else:
                distance = np.hypot(ix - source_x, iy - source_y)
                signal[ix, iy] = max(0, 100 - distance * attenuation_factor)
    return signal

# Assume source is at the center
source_x, source_y = grid_size // 2, grid_size // 2
signal_strength = wifi_propagation(environment, source_x, source_y)

plt.figure(figsize=(10, 10))
plt.imshow(signal_strength, origin='lower', cmap='hot', interpolation='none')
plt.colorbar(label='Signal Strength')
plt.title("WiFi Signal Propagation Considering Walls")
plt.show()
