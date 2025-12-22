
import open3d as o3d
import matplotlib.pyplot as plt
import numpy as np
import plotly.express as px
import pandas as pd
import os
from scipy.interpolate import griddata
import sys

xyz_file_path = '/Users/muctebadogru/Downloads/teknofest_ornek_data_v1/ornek_ev_1_lidar_export.xyz'
txt_file_path = '/Users/muctebadogru/Downloads/teknofest_ornek_data_v1/ornek_ev_1_lidar_export.txt'

point_cloud = np.loadtxt('ornek_ev_1_lidar_export.xyz', skiprows=1)

z_values = point_cloud[:, 2]  # Extract all z-values

max_height = np.max(z_values) - np.min(z_values)

print(f"The maximum height of the model is: {max_height} units")

point_cloud_o3d = o3d.geometry.PointCloud()

point_cloud_o3d.points = o3d.utility.Vector3dVector(point_cloud[:, :3])


#o3d.visualization.draw_geometries([point_cloud_o3d], window_name="Inlier Cloud after SORaa")

#clear data
cl, ind = point_cloud_o3d.remove_statistical_outlier(nb_neighbors=20, std_ratio=5.0)

#make smaller data
downsampled_point_cloud = cl.voxel_down_sample(voxel_size=0.02)

#remove ground
plane_model, inliers = downsampled_point_cloud.segment_plane(distance_threshold=0.13,
                                                             ransac_n=3,
                                                             num_iterations=100)
#line segmantation

#objet detaction

#2d map 


inlier_cloud = downsampled_point_cloud.select_by_index(inliers)
outlier_cloud = downsampled_point_cloud.select_by_index(inliers, invert=True)


#just walls 2 


min_height_threshold = 0.94  # Change this value based on your needs

# Filter points where the z-value is at least the minimum height
filtered_point_cloud = point_cloud[point_cloud[:, 2] >= min_height_threshold]


points_2d = filtered_point_cloud[:, :2]

# Optionally, plot the points to visualize the 2D projection
import matplotlib.pyplot as plt

figure_width = 8  
figure_height = 8  
point_size = 0.008  

plt.figure(figsize=(figure_width, figure_height))
plt.scatter(points_2d[:, 0], points_2d[:, 1], s=point_size) 
plt.axis('equal')
plt.xlabel('X')
plt.ylabel('Y')
plt.title('2D Projection of Point Cloud After Filtering by Height')
plt.show()


"""

remaining_points = outlier_cloud



# Initialize detected walls list
detected_walls = []

for i in range(100):
    if len(remaining_points.points) < 10000:  # If few points are left, stop the loop
        break

    # Segment a plane which is likely to be a wall
    plane_model, inliers = remaining_points.segment_plane(distance_threshold=0.02, ransac_n=3, num_iterations=1000)
    wall_candidate = remaining_points.select_by_index(inliers)

    # Calculate the height of the segmented plane
    bounding_box = wall_candidate.get_axis_aligned_bounding_box()
    min_bound = bounding_box.get_min_bound()  
    max_bound = bounding_box.get_max_bound()
    height = max_bound[2] - min_bound[2]

    # Check if the height is within the expected range of a wall
    if height > 2.0:
        detected_walls.append(wall_candidate)

    # Update remaining points
    remaining_points = remaining_points.select_by_index(inliers, invert=True)

# Visualize detected walls
for wall in detected_walls:
    o3d.visualization.draw_geometries([wall], window_name="Detected Wall")

"""



outlier_cloud_for_walls = outlier_cloud

#outlier_cloud_for_walls.estimate_normals(search_param=o3d.geometry.KDTreeSearchParamHybrid(radius=0.1, max_nn=30))

#o3d.visualization.draw_geometries([outlier_cloud_for_walls], window_name="Inlier Cloud after SORaa")


points_2d = np.asarray(outlier_cloud.points)[:, :2]


points_2d = np.asarray(outlier_cloud.points)[:, :2]

"""
fig = px.scatter(x=points_2d[:, 0], y=points_2d[:, 1], title='2D Projection of Point Cloud')
fig.update_layout(xaxis_title='X', yaxis_title='Y')
fig.show()

"""

figure_width = 8  
figure_height = 8  
point_size = 0.008  

plt.figure(figsize=(figure_width, figure_height))
plt.scatter(points_2d[:, 0], points_2d[:, 1], s=point_size) 
plt.axis('equal')
plt.xlabel('X')
plt.ylabel('Y')
plt.title('2D Projection of Point Cloud')
plt.show()



#wifi strenght

wifi_source = np.array([5, 5])
signal_strengths = np.linalg.norm(points_2d - wifi_source, axis=1)
signal_strengths = np.max(signal_strengths) - signal_strengths  # Invert so closer is stronger

# Normalize signal strengths for visualization
signal_strengths_normalized = (signal_strengths - np.min(signal_strengths)) / (np.max(signal_strengths) - np.min(signal_strengths))

grid_x, grid_y = np.mgrid[np.min(points_2d[:, 0]):np.max(points_2d[:, 0]):100j, np.min(points_2d[:, 1]):np.max(points_2d[:, 1]):100j]

# Using 'linear' interpolation this time for potentially less aggressive smoothing
grid_z = griddata(points_2d, signal_strengths_normalized, (grid_x, grid_y), method='linear')

# Adjust the number of levels in the contour plot
plt.figure(figsize=(figure_width, figure_height))
contour = plt.contourf(grid_x, grid_y, grid_z, levels=20, cmap='RdYlBu_r')  # Reduced number of levels
plt.colorbar(contour)
plt.title('Adjusted WiFi Signal Strength Heatmap')
plt.xlabel('X')
plt.ylabel('Y')
plt.axis('equal')
plt.show()

"""


# Create a DataFrame for Plotly
df = pd.DataFrame({
    'X': points_2d[:, 0],
    'Y': points_2d[:, 1],
    'Signal Strength': signal_strengths_normalized
})

fig = px.scatter(df, x='X', y='Y', color='Signal Strength',
                 color_continuous_scale='RdYlBu_r',  # Use a red-yellow-blue scale (reversed)
                 title='WiFi Heatmap on 2D Projection of Point Cloud')
fig.update_layout(coloraxis_colorbar=dict(title="Signal Strength"))
fig.update_traces(marker=dict(size=5))  # Adjust marker size as needed

fig.show()
"""

"""
#detect walls

# Parameters
distance_threshold = 0.02  # Adjust based on your data scale and precision requirements
ransac_n = 7
num_iterations = 10000



detected_walls = []

remaining_points = outlier_cloud

for i in range(100):  
    plane_model, inliers = remaining_points.segment_plane(distance_threshold=distance_threshold,
                                                          ransac_n=ransac_n,
                                                          num_iterations=num_iterations)
   
    wall = remaining_points.select_by_index(inliers)
    
    detected_walls.append(wall)
    
    
    remaining_points = remaining_points.select_by_index(inliers, invert=True)

    
    if len(remaining_points.points) < 10000:  
        break

for wall in detected_walls:
    o3d.visualization.draw_geometries([wall], window_name="Detected Wall")
    


"""




#o3d.visualization.draw_geometries([outlier_cloud], window_name="Inlier Cloud after SOR")






#size information



akp = outlier_cloud.get_axis_aligned_bounding_box()
min_bound = akp.get_min_bound()  
max_bound = akp.get_max_bound()  

length = max_bound[0] - min_bound[0]  
width = max_bound[1] - min_bound[1]  
height = max_bound[2] - min_bound[2] 




print(f"Length: {length} units, Width: {width} units, Height: {height} units")




"""
#to conver .txt
with open(xyz_file_path, 'r') as xyz_file:
    
    filtered_data = [' '.join(line.split()[:-3]) + '\n' for line in xyz_file]

with open(txt_file_path, 'w') as txt_file:
    txt_file.writelines(filtered_data)

print("Conversion completed. RGB columns have been excluded in", txt_file_path)
"""


#just walls

# Assuming `outlier_cloud` contains points after ground removal
remaining_points = outlier_cloud

# Initialize detected walls list
detected_walls = []

for i in range(100):
    if len(remaining_points.points) < 10000:  # If few points are left, stop the loop
        break

    # Segment a plane which is likely to be a wall
    plane_model, inliers = remaining_points.segment_plane(distance_threshold=0.02, ransac_n=3, num_iterations=1000)
    wall_candidate = remaining_points.select_by_index(inliers)

    # Calculate the height of the segmented plane
    bounding_box = wall_candidate.get_axis_aligned_bounding_box()
    min_bound = bounding_box.get_min_bound()  
    max_bound = bounding_box.get_max_bound()
    height = max_bound[2] - min_bound[2]

    # Check if the height is within the expected range of a wall
    if height > 2.590:
        detected_walls.append(wall_candidate)

    # Update remaining points
    remaining_points = remaining_points.select_by_index(inliers, invert=True)

# Visualize detected walls
for wall in detected_walls:
    o3d.visualization.draw_geometries([wall], window_name="Detected Wall")