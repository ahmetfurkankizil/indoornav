cmake_minimum_required(VERSION 3.10)
project(PhoneSLAMBridge)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_BUILD_TYPE Release)

# Find required packages
find_package(OpenCV REQUIRED)
find_package(Eigen3 REQUIRED)
find_package(Pangolin REQUIRED)

# ORB-SLAM3 paths
set(ORB_SLAM3_DIR "${CMAKE_SOURCE_DIR}/../ORB_SLAM3")
set(ORB_SLAM3_INCLUDE_DIRS
    ${ORB_SLAM3_DIR}
    ${ORB_SLAM3_DIR}/include
    ${ORB_SLAM3_DIR}/include/CameraModels
    ${ORB_SLAM3_DIR}/Thirdparty/Sophus
)

# ORB-SLAM3 library
set(ORB_SLAM3_LIBS
    ${ORB_SLAM3_DIR}/lib/libORB_SLAM3.so
)

# DBoW2 and g2o
set(DBOW2_LIBS ${ORB_SLAM3_DIR}/Thirdparty/DBoW2/lib/libDBoW2.so)
set(G2O_LIBS ${ORB_SLAM3_DIR}/Thirdparty/g2o/lib/libg2o.so)

# Include directories
include_directories(
    ${OpenCV_INCLUDE_DIRS}
    ${EIGEN3_INCLUDE_DIR}
    ${Pangolin_INCLUDE_DIRS}
    ${ORB_SLAM3_INCLUDE_DIRS}
)

# Phone SLAM Bridge executable (Termux version)
add_executable(phone_slam phone_slam.cpp)

target_link_libraries(phone_slam
    ${ORB_SLAM3_LIBS}
    ${DBOW2_LIBS}
    ${G2O_LIBS}
    ${OpenCV_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lGL -lGLEW
)

# Phone SLAM Bridge executable (IP Webcam version - easier setup)
add_executable(phone_slam_ipwebcam phone_slam_ipwebcam.cpp)

target_link_libraries(phone_slam_ipwebcam
    ${ORB_SLAM3_LIBS}
    ${DBOW2_LIBS}
    ${G2O_LIBS}
    ${OpenCV_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lGL -lGLEW
)

# Phone SLAM Bridge executable (Phyphox version - scientific app)
add_executable(phone_slam_phyphox phone_slam_phyphox.cpp)

target_link_libraries(phone_slam_phyphox
    ${ORB_SLAM3_LIBS}
    ${DBOW2_LIBS}
    ${G2O_LIBS}
    ${OpenCV_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lGL -lGLEW
)

# Phone SLAM Bridge executable (WebSocket version - for Sensor Server app)
add_executable(phone_slam_websocket phone_slam_websocket.cpp)

# Phone SLAM Bridge executable (Monocular only - no IMU, for testing)
add_executable(phone_slam_mono phone_slam_mono.cpp)

target_link_libraries(phone_slam_mono
    ${ORB_SLAM3_LIBS}
    ${DBOW2_LIBS}
    ${G2O_LIBS}
    ${OpenCV_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lGL -lGLEW
)

target_link_libraries(phone_slam_websocket
    ${ORB_SLAM3_LIBS}
    ${DBOW2_LIBS}
    ${G2O_LIBS}
    ${OpenCV_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lGL -lGLEW
)

# Set RPATH for finding ORB-SLAM3 libraries
set_target_properties(phone_slam phone_slam_ipwebcam phone_slam_phyphox phone_slam_websocket phone_slam_mono PROPERTIES
    BUILD_RPATH "${ORB_SLAM3_DIR}/lib;${ORB_SLAM3_DIR}/Thirdparty/DBoW2/lib;${ORB_SLAM3_DIR}/Thirdparty/g2o/lib"
    INSTALL_RPATH "${ORB_SLAM3_DIR}/lib;${ORB_SLAM3_DIR}/Thirdparty/DBoW2/lib;${ORB_SLAM3_DIR}/Thirdparty/g2o/lib"
)

message(STATUS "OpenCV: ${OpenCV_VERSION}")
message(STATUS "ORB-SLAM3: ${ORB_SLAM3_DIR}")

