import open3d as o3d
import numpy as np
import matplotlib.pyplot as plt

# Load point cloud
point_cloud = np.loadtxt('ornek_ev_1_lidar_export.xyz', skiprows=1)
point_cloud_o3d = o3d.geometry.PointCloud()
point_cloud_o3d.points = o3d.utility.Vector3dVector(point_cloud[:, :3])

# Preprocessing: Downsample and remove statistical outliers
downsampled_point_cloud = point_cloud_o3d.voxel_down_sample(voxel_size=0.05)
cleaned_point_cloud, ind = downsampled_point_cloud.remove_statistical_outlier(nb_neighbors=20, std_ratio=2.0)

# Remove ground and other horizontal planes
plane_model, inliers = cleaned_point_cloud.segment_plane(distance_threshold=0.13, ransac_n=3, num_iterations=10000)
floor = cleaned_point_cloud.select_by_index(inliers)
remaining_points = cleaned_point_cloud.select_by_index(inliers, invert=True)

# Detect walls by focusing on vertical structures
walls = []
while True:
    plane_model, inliers = remaining_points.segment_plane(distance_threshold=0.00002, ransac_n=3, num_iterations=10000)
    if len(inliers) < 100:  # Minimum points to consider a plane as a wall
        break
    segment = remaining_points.select_by_index(inliers)
    # Check if the plane is vertical
    normal = plane_model[:3]
    if abs(normal[2]) < 0.9:  # Vertical threshold, change as needed
        walls.append(segment)
    remaining_points = remaining_points.select_by_index(inliers, invert=True)

# Visualization of walls
if len(walls) > 0:
    o3d.visualization.draw_geometries(walls, window_name="Detected Walls")
else:
    print("No walls detected with the given parameters.")
