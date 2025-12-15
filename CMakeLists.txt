cmake_minimum_required(VERSION 3.10)
project(PhoneSlam)

# C++ Standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_BUILD_TYPE Release)

# Compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -march=native")

# ORB-SLAM3 path - adjust this to your installation
# Default assumes ORB_SLAM3 is in home directory
set(ORB_SLAM3_PATH "$ENV{HOME}/orbslam3_phone_demo/ORB_SLAM3" CACHE PATH "Path to ORB-SLAM3")

message(STATUS "ORB-SLAM3 path: ${ORB_SLAM3_PATH}")

# Check if ORB-SLAM3 exists
if(NOT EXISTS "${ORB_SLAM3_PATH}/include/System.h")
    message(FATAL_ERROR 
        "ORB-SLAM3 not found at ${ORB_SLAM3_PATH}\n"
        "Please set ORB_SLAM3_PATH to your ORB-SLAM3 installation directory:\n"
        "  cmake -DORB_SLAM3_PATH=/path/to/ORB_SLAM3 ..")
endif()

# Find required packages
find_package(OpenCV 4.0 REQUIRED)
find_package(Eigen3 3.1.0 REQUIRED)
find_package(Pangolin REQUIRED)

message(STATUS "OpenCV version: ${OpenCV_VERSION}")
message(STATUS "Eigen3 version: ${EIGEN3_VERSION_STRING}")
message(STATUS "Pangolin found: ${Pangolin_FOUND}")

# Include directories
include_directories(
    ${ORB_SLAM3_PATH}
    ${ORB_SLAM3_PATH}/include
    ${ORB_SLAM3_PATH}/include/CameraModels
    ${ORB_SLAM3_PATH}/Thirdparty/Sophus
    ${EIGEN3_INCLUDE_DIR}
    ${Pangolin_INCLUDE_DIRS}
    ${OpenCV_INCLUDE_DIRS}
)

# Link directories
link_directories(
    ${ORB_SLAM3_PATH}/lib
    ${ORB_SLAM3_PATH}/Thirdparty/DBoW2/lib
    ${ORB_SLAM3_PATH}/Thirdparty/g2o/lib
)

# ORB-SLAM3 libraries
set(ORB_SLAM3_LIBS
    ${ORB_SLAM3_PATH}/lib/libORB_SLAM3.so
    ${ORB_SLAM3_PATH}/Thirdparty/DBoW2/lib/libDBoW2.so
    ${ORB_SLAM3_PATH}/Thirdparty/g2o/lib/libg2o.so
)

# Build phone_mono executable
add_executable(phone_mono phone_mono.cc)

target_link_libraries(phone_mono
    ${ORB_SLAM3_LIBS}
    ${OpenCV_LIBS}
    ${EIGEN3_LIBS}
    ${Pangolin_LIBRARIES}
    pthread
    -lboost_serialization
    -lcrypto
)

# Copy necessary files to build directory
configure_file(${CMAKE_SOURCE_DIR}/phone_camera.yaml ${CMAKE_BINARY_DIR}/phone_camera.yaml COPYONLY)

# Create symlink to vocabulary
add_custom_command(TARGET phone_mono POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E create_symlink
        ${ORB_SLAM3_PATH}/Vocabulary
        ${CMAKE_BINARY_DIR}/Vocabulary
    COMMENT "Creating symlink to vocabulary"
)

# Installation
install(TARGETS phone_mono DESTINATION bin)
install(FILES phone_camera.yaml DESTINATION share/phone_slam)

# Print build summary
message(STATUS "")
message(STATUS "=======================================")
message(STATUS "Phone SLAM Build Configuration")
message(STATUS "=======================================")
message(STATUS "ORB-SLAM3 path:  ${ORB_SLAM3_PATH}")
message(STATUS "OpenCV:          ${OpenCV_VERSION}")
message(STATUS "Build type:      ${CMAKE_BUILD_TYPE}")
message(STATUS "=======================================")
message(STATUS "")

