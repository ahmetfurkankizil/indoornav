import open3d as o3d
import numpy as np

# Load your point cloud
point_cloud = o3d.io.read_point_cloud("/Users/muctebadogru/Downloads/teknofest_ornek_data_v1/ornek_ev_1_lidar_export.xyz", format='xyz')
o3d.visualization.draw_geometries([point_cloud], "Original Point Cloud")

# Downsample the point cloud using a voxel grid filter
voxel_size = 0.05  # Voxel size in meters
downsampled = point_cloud.voxel_down_sample(0.02)

# Remove statistical outliers
cleaned_point_cloud, ind = downsampled.remove_statistical_outlier(nb_neighbors=20, std_ratio=2.0)
o3d.visualization.draw_geometries([cleaned_point_cloud], "Filtered Point Cloud")

# Segment the ground plane
plane_model, inliers = cleaned_point_cloud.segment_plane(distance_threshold=0.13, ransac_n=3, num_iterations=1000)
ground = cleaned_point_cloud.select_by_index(inliers)
remaining_points = cleaned_point_cloud.select_by_index(inliers, invert=True)

# Optionally visualize the ground and remaining points
o3d.visualization.draw_geometries([ground], "Ground Plane")
o3d.visualization.draw_geometries([remaining_points], "Remaining Points")

walls = []
while len(remaining_points.points) > 1000:  # Ensure there are enough points to process
    plane_model, inliers = remaining_points.segment_plane(distance_threshold=0.01, ransac_n=3, num_iterations=1000)
    wall = remaining_points.select_by_index(inliers)
    
    # Check if the normal vector is vertical
    if abs(plane_model[2]) > 0.9:  # Adjust this value based on your data orientation
        walls.append(wall)
        
    remaining_points = remaining_points.select_by_index(inliers, invert=True)

# Visualize detected walls
if walls:
    o3d.visualization.draw_geometries(walls, "Detected Walls")
else:
    print("No vertical planes detected.")
