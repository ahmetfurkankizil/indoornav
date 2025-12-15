/**
 * Phone SLAM Bridge - Phyphox + IP Webcam Version
 * 
 * Uses:
 *   - IP Webcam app for camera streaming
 *   - Phyphox app for IMU data (via HTTP API)
 * 
 * Both apps are free on Google Play Store and require zero configuration.
 * 
 * Setup:
 *   1. Install IP Webcam and Phyphox from Play Store
 *   2. IP Webcam: Start server, note the URL
 *   3. Phyphox: Enable Remote Access, start "Accelerometer & Gyroscope" experiment
 * 
 * Usage:
 *   ./phone_slam_phyphox <vocabulary> <settings> <ip_webcam_url> <phyphox_url>
 *   
 *   Example:
 *   ./phone_slam_phyphox ../ORB_SLAM3/Vocabulary/ORBvoc.txt ../configs/samsung_s23fe.yaml \
 *       http://192.168.1.100:8080/video http://192.168.1.100:8081
 */

#include <iostream>
#include <thread>
#include <mutex>
#include <atomic>
#include <chrono>
#include <cstring>
#include <vector>
#include <algorithm>
#include <sstream>
#include <regex>

// Network
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>

// OpenCV
#include <opencv2/opencv.hpp>

// ORB-SLAM3
#include "System.h"
#include "ImuTypes.h"

// IMU measurement struct
struct ImuMeasurement {
    double timestamp;
    float ax, ay, az;  // Accelerometer (m/s^2)
    float gx, gy, gz;  // Gyroscope (rad/s)
};

// Global state
std::vector<ImuMeasurement> g_imu_buffer;
std::mutex g_imu_mutex;
std::atomic<bool> g_running(true);

// Simple HTTP GET request
std::string httpGet(const std::string& host, int port, const std::string& path) {
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) return "";
    
    // Set timeout
    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
    
    struct hostent* server = gethostbyname(host.c_str());
    if (!server) {
        close(sock);
        return "";
    }
    
    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    memcpy(&addr.sin_addr.s_addr, server->h_addr, server->h_length);
    
    if (connect(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        close(sock);
        return "";
    }
    
    // Send HTTP request
    std::string request = "GET " + path + " HTTP/1.1\r\n"
                         "Host: " + host + "\r\n"
                         "Connection: close\r\n\r\n";
    send(sock, request.c_str(), request.size(), 0);
    
    // Receive response
    std::string response;
    char buffer[4096];
    int n;
    while ((n = recv(sock, buffer, sizeof(buffer)-1, 0)) > 0) {
        buffer[n] = '\0';
        response += buffer;
    }
    
    close(sock);
    
    // Extract body (after \r\n\r\n)
    size_t pos = response.find("\r\n\r\n");
    if (pos != std::string::npos) {
        return response.substr(pos + 4);
    }
    return response;
}

// Parse URL into host, port, path
bool parseUrl(const std::string& url, std::string& host, int& port, std::string& path) {
    // http://host:port/path
    std::regex urlRegex("http://([^:/]+):?(\\d*)(.*)");
    std::smatch match;
    
    if (std::regex_match(url, match, urlRegex)) {
        host = match[1];
        port = match[2].length() > 0 ? std::stoi(match[2]) : 80;
        path = match[3].length() > 0 ? match[3].str() : "/";
        return true;
    }
    return false;
}

// Parse phyphox JSON response for sensor data
// Example response: {"buffer":{"acc_x":{"buffer":[0.1],"size":1},...}}
bool parsePhyphoxData(const std::string& json, ImuMeasurement& imu) {
    // Simple JSON parsing for phyphox format
    auto extractValue = [&json](const std::string& key) -> float {
        std::string searchKey = "\"" + key + "\":{\"buffer\":[";
        size_t pos = json.find(searchKey);
        if (pos != std::string::npos) {
            pos += searchKey.length();
            size_t endPos = json.find("]", pos);
            if (endPos != std::string::npos) {
                std::string valueStr = json.substr(pos, endPos - pos);
                // Get last value if multiple
                size_t lastComma = valueStr.rfind(",");
                if (lastComma != std::string::npos) {
                    valueStr = valueStr.substr(lastComma + 1);
                }
                try {
                    return std::stof(valueStr);
                } catch (...) {
                    return 0.0f;
                }
            }
        }
        return 0.0f;
    };
    
    // Phyphox buffer names vary by experiment
    // Try common names
    imu.ax = extractValue("accX");
    if (imu.ax == 0) imu.ax = extractValue("acc_x");
    if (imu.ax == 0) imu.ax = extractValue("accelerationX");
    
    imu.ay = extractValue("accY");
    if (imu.ay == 0) imu.ay = extractValue("acc_y");
    if (imu.ay == 0) imu.ay = extractValue("accelerationY");
    
    imu.az = extractValue("accZ");
    if (imu.az == 0) imu.az = extractValue("acc_z");
    if (imu.az == 0) imu.az = extractValue("accelerationZ");
    
    imu.gx = extractValue("gyroX");
    if (imu.gx == 0) imu.gx = extractValue("gyro_x");
    if (imu.gx == 0) imu.gx = extractValue("gyroscopeX");
    
    imu.gy = extractValue("gyroY");
    if (imu.gy == 0) imu.gy = extractValue("gyro_y");
    if (imu.gy == 0) imu.gy = extractValue("gyroscopeY");
    
    imu.gz = extractValue("gyroZ");
    if (imu.gz == 0) imu.gz = extractValue("gyro_z");
    if (imu.gz == 0) imu.gz = extractValue("gyroscopeZ");
    
    return true;
}

// Phyphox IMU poller thread
void phyphoxPollerThread(const std::string& phyphoxUrl) {
    std::string host;
    int port;
    std::string basePath;
    
    if (!parseUrl(phyphoxUrl, host, port, basePath)) {
        std::cerr << "Invalid phyphox URL: " << phyphoxUrl << std::endl;
        return;
    }
    
    std::cout << "Polling phyphox at " << host << ":" << port << std::endl;
    
    // Phyphox API endpoint for getting buffer data
    std::string apiPath = "/get?accX&accY&accZ&gyroX&gyroY&gyroZ";
    
    int poll_rate_ms = 5;  // Poll at ~200Hz
    int success_count = 0;
    int fail_count = 0;
    
    while (g_running) {
        auto start = std::chrono::steady_clock::now();
        
        std::string response = httpGet(host, port, apiPath);
        
        if (!response.empty()) {
            ImuMeasurement imu;
            imu.timestamp = std::chrono::duration<double>(
                std::chrono::system_clock::now().time_since_epoch()).count();
            
            if (parsePhyphoxData(response, imu)) {
                std::lock_guard<std::mutex> lock(g_imu_mutex);
                g_imu_buffer.push_back(imu);
                
                if (g_imu_buffer.size() > 10000) {
                    g_imu_buffer.erase(g_imu_buffer.begin(), g_imu_buffer.begin() + 5000);
                }
                success_count++;
            }
        } else {
            fail_count++;
            if (fail_count % 100 == 0) {
                std::cerr << "Phyphox connection issues. Make sure Remote Access is enabled." << std::endl;
            }
        }
        
        // Print status periodically
        if ((success_count + fail_count) % 500 == 0 && success_count > 0) {
            std::cout << "Phyphox: " << success_count << " readings, " 
                      << (success_count * 100 / (success_count + fail_count)) << "% success" << std::endl;
        }
        
        // Maintain poll rate
        auto elapsed = std::chrono::steady_clock::now() - start;
        auto sleep_time = std::chrono::milliseconds(poll_rate_ms) - elapsed;
        if (sleep_time.count() > 0) {
            std::this_thread::sleep_for(sleep_time);
        }
    }
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
    std::cout << "Usage: " << prog << " <vocabulary> <settings> <ip_webcam_url> <phyphox_url>" << std::endl;
    std::cout << std::endl;
    std::cout << "Arguments:" << std::endl;
    std::cout << "  vocabulary     Path to ORB vocabulary file" << std::endl;
    std::cout << "  settings       Path to camera/IMU settings YAML file" << std::endl;
    std::cout << "  ip_webcam_url  URL from IP Webcam (e.g., http://192.168.1.100:8080/video)" << std::endl;
    std::cout << "  phyphox_url    URL from Phyphox (e.g., http://192.168.1.100:8081)" << std::endl;
    std::cout << std::endl;
    std::cout << "Setup:" << std::endl;
    std::cout << "  1. Install IP Webcam and Phyphox from Play Store" << std::endl;
    std::cout << "  2. IP Webcam: Start server" << std::endl;
    std::cout << "  3. Phyphox: Menu -> Remote Access -> Enable" << std::endl;
    std::cout << "  4. Phyphox: Start 'Accelerometer & Gyroscope' or similar experiment" << std::endl;
}

int main(int argc, char** argv) {
    if (argc < 5) {
        printUsage(argv[0]);
        return 1;
    }
    
    std::string vocab_path = argv[1];
    std::string settings_path = argv[2];
    std::string video_url = argv[3];
    std::string phyphox_url = argv[4];
    
    std::cout << "=== Phone SLAM (Phyphox Version) ===" << std::endl;
    std::cout << "Vocabulary: " << vocab_path << std::endl;
    std::cout << "Settings: " << settings_path << std::endl;
    std::cout << "Video URL: " << video_url << std::endl;
    std::cout << "Phyphox URL: " << phyphox_url << std::endl;
    std::cout << std::endl;
    
    // Initialize ORB-SLAM3
    std::cout << "Initializing ORB-SLAM3..." << std::endl;
    ORB_SLAM3::System SLAM(vocab_path, settings_path, ORB_SLAM3::System::IMU_MONOCULAR, true);
    
    // Start phyphox poller thread
    std::thread phyphox_thread(phyphoxPollerThread, phyphox_url);
    
    // Open video stream from IP Webcam
    std::cout << "Connecting to IP Webcam..." << std::endl;
    cv::VideoCapture cap(video_url);
    
    if (!cap.isOpened()) {
        std::cerr << "Failed to open video stream: " << video_url << std::endl;
        g_running = false;
        phyphox_thread.join();
        SLAM.Shutdown();
        return 1;
    }
    
    std::cout << "Connected!" << std::endl;
    std::cout << "Press Ctrl+C to stop" << std::endl;
    
    double last_frame_time = 0;
    int frame_count = 0;
    auto start_time = std::chrono::steady_clock::now();
    
    cv::Mat frame, gray;
    
    while (g_running) {
        if (!cap.read(frame)) {
            std::cerr << "Failed to read frame" << std::endl;
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }
        
        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);
        
        double timestamp = std::chrono::duration<double>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        
        std::vector<ORB_SLAM3::IMU::Point> imu_measurements;
        if (last_frame_time > 0) {
            imu_measurements = getImuMeasurements(last_frame_time, timestamp);
        }
        
        if (imu_measurements.size() >= 2) {
            SLAM.TrackMonocular(gray, timestamp, imu_measurements);
        } else if (last_frame_time == 0) {
            std::vector<ORB_SLAM3::IMU::Point> empty_imu;
            SLAM.TrackMonocular(gray, timestamp, empty_imu);
        }
        
        last_frame_time = timestamp;
        frame_count++;
        
        if (frame_count % 100 == 0) {
            auto now = std::chrono::steady_clock::now();
            double elapsed = std::chrono::duration<double>(now - start_time).count();
            size_t imu_size;
            {
                std::lock_guard<std::mutex> lock(g_imu_mutex);
                imu_size = g_imu_buffer.size();
            }
            std::cout << "Frames: " << frame_count 
                      << ", FPS: " << (frame_count / elapsed)
                      << ", IMU buffer: " << imu_size << std::endl;
        }
        
        if (cv::waitKey(1) == 'q') break;
    }
    
    std::cout << "Shutting down..." << std::endl;
    
    g_running = false;
    phyphox_thread.join();
    cap.release();
    
    SLAM.Shutdown();
    
    SLAM.SaveTrajectoryTUM("trajectory_phone.txt");
    SLAM.SaveKeyFrameTrajectoryTUM("keyframe_trajectory_phone.txt");
    
    std::cout << "Trajectory saved!" << std::endl;
    
    return 0;
}

