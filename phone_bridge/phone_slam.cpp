/**
 * Phone SLAM Bridge
 * 
 * Receives camera frames and IMU data from Android phone and runs ORB-SLAM3
 * Monocular-Inertial mode.
 * 
 * Build:
 *   mkdir build && cd build
 *   cmake .. && make
 * 
 * Usage:
 *   ./phone_slam <vocabulary> <settings> [camera_port] [imu_port]
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

// Frame with timestamp
struct TimestampedFrame {
    double timestamp;
    cv::Mat image;
};

// Thread-safe queue
template<typename T>
class SafeQueue {
public:
    void push(const T& item) {
        std::lock_guard<std::mutex> lock(mutex_);
        queue_.push(item);
        if (queue_.size() > max_size_) {
            queue_.pop();  // Drop oldest if queue is full
        }
    }
    
    bool pop(T& item) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (queue_.empty()) return false;
        item = queue_.front();
        queue_.pop();
        return true;
    }
    
    bool empty() {
        std::lock_guard<std::mutex> lock(mutex_);
        return queue_.empty();
    }
    
    size_t size() {
        std::lock_guard<std::mutex> lock(mutex_);
        return queue_.size();
    }
    
    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        while (!queue_.empty()) queue_.pop();
    }
    
private:
    std::queue<T> queue_;
    std::mutex mutex_;
    size_t max_size_ = 100;
};

// Global state
std::atomic<bool> g_running(true);
SafeQueue<TimestampedFrame> g_frame_queue;
SafeQueue<ImuMeasurement> g_imu_queue;
std::vector<ImuMeasurement> g_imu_buffer;
std::mutex g_imu_mutex;

// Camera receiver thread (TCP)
void cameraReceiverThread(int port) {
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        std::cerr << "Failed to create camera socket" << std::endl;
        return;
    }
    
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    
    struct sockaddr_in address;
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(port);
    
    if (bind(server_fd, (struct sockaddr*)&address, sizeof(address)) < 0) {
        std::cerr << "Failed to bind camera socket to port " << port << std::endl;
        close(server_fd);
        return;
    }
    
    listen(server_fd, 1);
    std::cout << "Camera receiver listening on port " << port << std::endl;
    
    while (g_running) {
        socklen_t addrlen = sizeof(address);
        int client_fd = accept(server_fd, (struct sockaddr*)&address, &addrlen);
        
        if (client_fd < 0) {
            if (g_running) std::cerr << "Accept failed" << std::endl;
            continue;
        }
        
        std::cout << "Camera client connected" << std::endl;
        
        while (g_running) {
            // Read header: timestamp (8 bytes) + size (4 bytes)
            char header[12];
            int bytes_read = 0;
            while (bytes_read < 12) {
                int n = recv(client_fd, header + bytes_read, 12 - bytes_read, 0);
                if (n <= 0) break;
                bytes_read += n;
            }
            
            if (bytes_read < 12) {
                std::cout << "Camera client disconnected" << std::endl;
                break;
            }
            
            double timestamp;
            uint32_t size;
            std::memcpy(&timestamp, header, 8);
            std::memcpy(&size, header + 8, 4);
            
            // Read JPEG data
            std::vector<uint8_t> jpeg_data(size);
            bytes_read = 0;
            while (bytes_read < (int)size) {
                int n = recv(client_fd, jpeg_data.data() + bytes_read, size - bytes_read, 0);
                if (n <= 0) break;
                bytes_read += n;
            }
            
            if (bytes_read < (int)size) {
                std::cout << "Incomplete frame received" << std::endl;
                break;
            }
            
            // Decode JPEG
            cv::Mat frame = cv::imdecode(jpeg_data, cv::IMREAD_GRAYSCALE);
            if (!frame.empty()) {
                TimestampedFrame tf;
                tf.timestamp = timestamp;
                tf.image = frame;
                g_frame_queue.push(tf);
            }
        }
        
        close(client_fd);
    }
    
    close(server_fd);
}

// IMU receiver thread (UDP)
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
    
    std::cout << "IMU receiver listening on port " << port << std::endl;
    
    // Set receive timeout
    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    setsockopt(sock_fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    
    char buffer[32];  // timestamp (8) + accel (12) + gyro (12) = 32 bytes
    
    while (g_running) {
        int n = recv(sock_fd, buffer, 32, 0);
        if (n == 32) {
            ImuMeasurement imu;
            std::memcpy(&imu.timestamp, buffer, 8);
            std::memcpy(&imu.ax, buffer + 8, 4);
            std::memcpy(&imu.ay, buffer + 12, 4);
            std::memcpy(&imu.az, buffer + 16, 4);
            std::memcpy(&imu.gx, buffer + 20, 4);
            std::memcpy(&imu.gy, buffer + 24, 4);
            std::memcpy(&imu.gz, buffer + 28, 4);
            
            // Store in buffer for SLAM
            std::lock_guard<std::mutex> lock(g_imu_mutex);
            g_imu_buffer.push_back(imu);
            
            // Keep buffer size reasonable
            if (g_imu_buffer.size() > 10000) {
                g_imu_buffer.erase(g_imu_buffer.begin(), g_imu_buffer.begin() + 5000);
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
            // Convert to ORB-SLAM3 IMU format
            // Note: May need to adjust coordinate system based on phone orientation
            ORB_SLAM3::IMU::Point point(
                imu.ax, imu.ay, imu.az,   // Accelerometer (m/s^2)
                imu.gx, imu.gy, imu.gz,   // Gyroscope (rad/s)
                imu.timestamp
            );
            measurements.push_back(point);
        }
    }
    
    // Remove old measurements
    auto it = std::remove_if(g_imu_buffer.begin(), g_imu_buffer.end(),
        [t_start](const ImuMeasurement& m) { return m.timestamp < t_start - 1.0; });
    g_imu_buffer.erase(it, g_imu_buffer.end());
    
    return measurements;
}

void printUsage(const char* prog) {
    std::cout << "Usage: " << prog << " <vocabulary> <settings> [camera_port] [imu_port]" << std::endl;
    std::cout << std::endl;
    std::cout << "Arguments:" << std::endl;
    std::cout << "  vocabulary   Path to ORB vocabulary file (ORBvoc.txt)" << std::endl;
    std::cout << "  settings     Path to camera/IMU settings YAML file" << std::endl;
    std::cout << "  camera_port  TCP port for camera stream (default: 5000)" << std::endl;
    std::cout << "  imu_port     UDP port for IMU stream (default: 5001)" << std::endl;
}

int main(int argc, char** argv) {
    if (argc < 3) {
        printUsage(argv[0]);
        return 1;
    }
    
    std::string vocab_path = argv[1];
    std::string settings_path = argv[2];
    int camera_port = argc > 3 ? std::atoi(argv[3]) : 5000;
    int imu_port = argc > 4 ? std::atoi(argv[4]) : 5001;
    
    std::cout << "=== Phone SLAM Bridge ===" << std::endl;
    std::cout << "Vocabulary: " << vocab_path << std::endl;
    std::cout << "Settings: " << settings_path << std::endl;
    std::cout << "Camera port: " << camera_port << std::endl;
    std::cout << "IMU port: " << imu_port << std::endl;
    std::cout << std::endl;
    
    // Initialize ORB-SLAM3
    std::cout << "Initializing ORB-SLAM3..." << std::endl;
    ORB_SLAM3::System SLAM(vocab_path, settings_path, ORB_SLAM3::System::IMU_MONOCULAR, true);
    
    // Start receiver threads
    std::thread camera_thread(cameraReceiverThread, camera_port);
    std::thread imu_thread(imuReceiverThread, imu_port);
    
    std::cout << "Waiting for phone connection..." << std::endl;
    std::cout << "Start the streaming app on your phone with:" << std::endl;
    std::cout << "  python stream_sensors.py --host <THIS_PC_IP> --camera-port " 
              << camera_port << " --imu-port " << imu_port << std::endl;
    std::cout << std::endl;
    std::cout << "Press Ctrl+C to stop" << std::endl;
    
    double last_frame_time = 0;
    int frame_count = 0;
    auto start_time = std::chrono::steady_clock::now();
    
    while (g_running) {
        TimestampedFrame frame;
        if (g_frame_queue.pop(frame)) {
            // Get IMU measurements since last frame
            std::vector<ORB_SLAM3::IMU::Point> imu_measurements;
            if (last_frame_time > 0) {
                imu_measurements = getImuMeasurements(last_frame_time, frame.timestamp);
            }
            
            // Process frame with IMU
            if (imu_measurements.size() >= 2) {
                SLAM.TrackMonocular(frame.image, frame.timestamp, imu_measurements);
            } else if (last_frame_time == 0) {
                // First frame, no IMU needed
                std::vector<ORB_SLAM3::IMU::Point> empty_imu;
                SLAM.TrackMonocular(frame.image, frame.timestamp, empty_imu);
            }
            
            last_frame_time = frame.timestamp;
            frame_count++;
            
            // Print FPS every 100 frames
            if (frame_count % 100 == 0) {
                auto now = std::chrono::steady_clock::now();
                double elapsed = std::chrono::duration<double>(now - start_time).count();
                std::cout << "Processed " << frame_count << " frames, FPS: " 
                          << (frame_count / elapsed) << std::endl;
            }
        } else {
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        }
    }
    
    std::cout << "Shutting down..." << std::endl;
    
    g_running = false;
    camera_thread.join();
    imu_thread.join();
    
    SLAM.Shutdown();
    
    // Save trajectory
    SLAM.SaveTrajectoryTUM("trajectory_phone.txt");
    SLAM.SaveKeyFrameTrajectoryTUM("keyframe_trajectory_phone.txt");
    
    std::cout << "Trajectory saved to trajectory_phone.txt" << std::endl;
    
    return 0;
}

