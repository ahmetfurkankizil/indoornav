/**
 * Phone SLAM Bridge - IP Webcam Version
 * 
 * Uses IP Webcam Android app for camera streaming and Sensor Server for IMU.
 * This is an easier setup than the Termux-based solution.
 * 
 * Required Android Apps:
 *   1. IP Webcam (from Play Store) - for camera
 *   2. Sensor Server or Wireless IMU (from Play Store) - for IMU
 * 
 * Build:
 *   mkdir build && cd build
 *   cmake .. && make
 * 
 * Usage:
 *   ./phone_slam_ipwebcam <vocabulary> <settings> <ip_webcam_url> [imu_port]
 *   
 *   Example:
 *   ./phone_slam_ipwebcam ../ORB_SLAM3/Vocabulary/ORBvoc.txt ../configs/samsung_s23fe.yaml \
 *       http://192.168.1.100:8080/video 5001
 */

#include <iostream>
#include <thread>
#include <mutex>
#include <queue>
#include <atomic>
#include <chrono>
#include <cstring>
#include <vector>
#include <algorithm>

// Network
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>

// OpenCV
#include <opencv2/opencv.hpp>

// ORB-SLAM3
#include "System.h"
#include "ImuTypes.h"

// IMU measurement struct
struct ImuMeasurement {
    double timestamp;
    float ax, ay, az;  // Accelerometer
    float gx, gy, gz;  // Gyroscope
};

// Thread-safe IMU buffer
std::vector<ImuMeasurement> g_imu_buffer;
std::mutex g_imu_mutex;
std::atomic<bool> g_running(true);

// IMU receiver thread (UDP) - compatible with Sensor Server app
void imuReceiverThread(int port) {
    int sock_fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock_fd < 0) {
        std::cerr << "Failed to create IMU socket" << std::endl;
        return;
    }
    
    struct sockaddr_in address;
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(port);
    
    if (bind(sock_fd, (struct sockaddr*)&address, sizeof(address)) < 0) {
        std::cerr << "Failed to bind IMU socket to port " << port << std::endl;
        close(sock_fd);
        return;
    }
    
    std::cout << "IMU receiver listening on UDP port " << port << std::endl;
    
    // Set receive timeout
    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    setsockopt(sock_fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    
    char buffer[256];
    
    while (g_running) {
        struct sockaddr_in client_addr;
        socklen_t addr_len = sizeof(client_addr);
        
        int n = recvfrom(sock_fd, buffer, sizeof(buffer)-1, 0, 
                         (struct sockaddr*)&client_addr, &addr_len);
        
        if (n > 0) {
            buffer[n] = '\0';
            
            // Parse different IMU data formats
            ImuMeasurement imu;
            imu.timestamp = std::chrono::duration<double>(
                std::chrono::system_clock::now().time_since_epoch()).count();
            
            // Try parsing as CSV: ax,ay,az,gx,gy,gz
            if (sscanf(buffer, "%f,%f,%f,%f,%f,%f",
                       &imu.ax, &imu.ay, &imu.az,
                       &imu.gx, &imu.gy, &imu.gz) == 6) {
                std::lock_guard<std::mutex> lock(g_imu_mutex);
                g_imu_buffer.push_back(imu);
                
                if (g_imu_buffer.size() > 10000) {
                    g_imu_buffer.erase(g_imu_buffer.begin(), g_imu_buffer.begin() + 5000);
                }
            }
            // Try parsing Sensor Server JSON format
            else {
                // Simple JSON parsing for Sensor Server format
                // {"accelerometer":{"x":0.1,"y":0.2,"z":9.8},"gyroscope":{"x":0.0,"y":0.0,"z":0.0}}
                float values[6] = {0};
                const char* accel = strstr(buffer, "\"accelerometer\"");
                const char* gyro = strstr(buffer, "\"gyroscope\"");
                
                if (accel && gyro) {
                    sscanf(accel, "\"accelerometer\":{\"x\":%f,\"y\":%f,\"z\":%f}",
                           &values[0], &values[1], &values[2]);
                    sscanf(gyro, "\"gyroscope\":{\"x\":%f,\"y\":%f,\"z\":%f}",
                           &values[3], &values[4], &values[5]);
                    
                    imu.ax = values[0]; imu.ay = values[1]; imu.az = values[2];
                    imu.gx = values[3]; imu.gy = values[4]; imu.gz = values[5];
                    
                    std::lock_guard<std::mutex> lock(g_imu_mutex);
                    g_imu_buffer.push_back(imu);
                    
                    if (g_imu_buffer.size() > 10000) {
                        g_imu_buffer.erase(g_imu_buffer.begin(), g_imu_buffer.begin() + 5000);
                    }
                }
            }
        }
    }
    
    close(sock_fd);
}

// Get IMU measurements between two timestamps
std::vector<ORB_SLAM3::IMU::Point> getImuMeasurements(double t_start, double t_end) {
    std::vector<ORB_SLAM3::IMU::Point> measurements;
    
    std::lock_guard<std::mutex> lock(g_imu_mutex);
    
    for (const auto& imu : g_imu_buffer) {
        if (imu.timestamp > t_start && imu.timestamp <= t_end) {
            ORB_SLAM3::IMU::Point point(
                imu.ax, imu.ay, imu.az,
                imu.gx, imu.gy, imu.gz,
                imu.timestamp
            );
            measurements.push_back(point);
        }
    }
    
    // Clean old measurements
    auto it = std::remove_if(g_imu_buffer.begin(), g_imu_buffer.end(),
        [t_start](const ImuMeasurement& m) { return m.timestamp < t_start - 1.0; });
    g_imu_buffer.erase(it, g_imu_buffer.end());
    
    return measurements;
}

void printUsage(const char* prog) {
    std::cout << "Usage: " << prog << " <vocabulary> <settings> <ip_webcam_url> [imu_port]" << std::endl;
    std::cout << std::endl;
    std::cout << "Arguments:" << std::endl;
    std::cout << "  vocabulary     Path to ORB vocabulary file" << std::endl;
    std::cout << "  settings       Path to camera/IMU settings YAML file" << std::endl;
    std::cout << "  ip_webcam_url  URL from IP Webcam app (e.g., http://192.168.1.100:8080/video)" << std::endl;
    std::cout << "  imu_port       UDP port for IMU data (default: 5001)" << std::endl;
    std::cout << std::endl;
    std::cout << "Required Android Apps:" << std::endl;
    std::cout << "  1. IP Webcam - Start server, use the video URL shown" << std::endl;
    std::cout << "  2. Sensor Server or Wireless IMU - Configure to send UDP to this PC" << std::endl;
}

int main(int argc, char** argv) {
    if (argc < 4) {
        printUsage(argv[0]);
        return 1;
    }
    
    std::string vocab_path = argv[1];
    std::string settings_path = argv[2];
    std::string video_url = argv[3];
    int imu_port = argc > 4 ? std::atoi(argv[4]) : 5001;
    
    std::cout << "=== Phone SLAM (IP Webcam Version) ===" << std::endl;
    std::cout << "Vocabulary: " << vocab_path << std::endl;
    std::cout << "Settings: " << settings_path << std::endl;
    std::cout << "Video URL: " << video_url << std::endl;
    std::cout << "IMU port: " << imu_port << std::endl;
    std::cout << std::endl;
    
    // Initialize ORB-SLAM3
    std::cout << "Initializing ORB-SLAM3..." << std::endl;
    ORB_SLAM3::System SLAM(vocab_path, settings_path, ORB_SLAM3::System::IMU_MONOCULAR, true);
    
    // Start IMU receiver thread
    std::thread imu_thread(imuReceiverThread, imu_port);
    
    // Open video stream from IP Webcam
    std::cout << "Connecting to IP Webcam at " << video_url << "..." << std::endl;
    cv::VideoCapture cap(video_url);
    
    if (!cap.isOpened()) {
        std::cerr << "Failed to open video stream!" << std::endl;
        std::cerr << "Make sure IP Webcam is running and the URL is correct." << std::endl;
        g_running = false;
        imu_thread.join();
        SLAM.Shutdown();
        return 1;
    }
    
    std::cout << "Connected to IP Webcam!" << std::endl;
    std::cout << "Press Ctrl+C to stop" << std::endl;
    
    double last_frame_time = 0;
    int frame_count = 0;
    auto start_time = std::chrono::steady_clock::now();
    
    cv::Mat frame, gray;
    
    while (g_running) {
        if (!cap.read(frame)) {
            std::cerr << "Failed to read frame" << std::endl;
            break;
        }
        
        // Convert to grayscale
        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);
        
        // Get timestamp
        double timestamp = std::chrono::duration<double>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        
        // Get IMU measurements since last frame
        std::vector<ORB_SLAM3::IMU::Point> imu_measurements;
        if (last_frame_time > 0) {
            imu_measurements = getImuMeasurements(last_frame_time, timestamp);
        }
        
        // Process frame with IMU
        if (imu_measurements.size() >= 2) {
            SLAM.TrackMonocular(gray, timestamp, imu_measurements);
        } else if (last_frame_time == 0) {
            // First frame
            std::vector<ORB_SLAM3::IMU::Point> empty_imu;
            SLAM.TrackMonocular(gray, timestamp, empty_imu);
        }
        
        last_frame_time = timestamp;
        frame_count++;
        
        // Print FPS every 100 frames
        if (frame_count % 100 == 0) {
            auto now = std::chrono::steady_clock::now();
            double elapsed = std::chrono::duration<double>(now - start_time).count();
            std::cout << "Processed " << frame_count << " frames, FPS: " 
                      << (frame_count / elapsed) 
                      << ", IMU buffer: " << g_imu_buffer.size() << std::endl;
        }
        
        // Check for 'q' key to quit
        if (cv::waitKey(1) == 'q') {
            break;
        }
    }
    
    std::cout << "Shutting down..." << std::endl;
    
    g_running = false;
    imu_thread.join();
    cap.release();
    
    SLAM.Shutdown();
    
    // Save trajectory
    SLAM.SaveTrajectoryTUM("trajectory_phone.txt");
    SLAM.SaveKeyFrameTrajectoryTUM("keyframe_trajectory_phone.txt");
    
    std::cout << "Trajectory saved!" << std::endl;
    
    return 0;
}

