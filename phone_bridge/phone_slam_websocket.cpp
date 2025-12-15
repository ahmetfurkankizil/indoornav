/**
 * Phone SLAM Bridge - WebSocket Version
 * 
 * Uses:
 *   - IP Webcam app for camera streaming (HTTP)
 *   - Sensor Server app for IMU data (WebSocket)
 * 
 * Usage:
 *   ./phone_slam_websocket <vocabulary> <settings> <camera_url> <websocket_host> <websocket_port>
 *   
 *   Example:
 *   ./phone_slam_websocket ../ORB_SLAM3/Vocabulary/ORBvoc.txt ../configs/samsung_s23fe.yaml \
 *       http://10.201.199.197:8085/video 10.201.199.197 8081
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
#include <random>
#include <iomanip>

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

// Base64 encoding for WebSocket handshake
static const std::string base64_chars = 
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

std::string base64_encode(const unsigned char* data, size_t len) {
    std::string ret;
    int i = 0, j = 0;
    unsigned char char_array_3[3], char_array_4[4];
    
    while (len--) {
        char_array_3[i++] = *(data++);
        if (i == 3) {
            char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
            char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
            char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
            char_array_4[3] = char_array_3[2] & 0x3f;
            for (i = 0; i < 4; i++) ret += base64_chars[char_array_4[i]];
            i = 0;
        }
    }
    if (i) {
        for (j = i; j < 3; j++) char_array_3[j] = '\0';
        char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
        char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
        char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
        for (j = 0; j < i + 1; j++) ret += base64_chars[char_array_4[j]];
        while (i++ < 3) ret += '=';
    }
    return ret;
}

// Generate random WebSocket key
std::string generateWebSocketKey() {
    unsigned char key[16];
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 255);
    for (int i = 0; i < 16; i++) key[i] = dis(gen);
    return base64_encode(key, 16);
}

// Storage for accumulating sensor data (accelerometer and gyroscope come separately)
struct SensorAccumulator {
    float ax = 0, ay = 0, az = 0;
    float gx = 0, gy = 0, gz = 0;
    bool hasAccel = false;
    bool hasGyro = false;
    double lastAccelTime = 0;
    double lastGyroTime = 0;
};

SensorAccumulator g_sensor_accum;
std::mutex g_accum_mutex;

// Extract a float value after a key in JSON
float extractJsonFloat(const std::string& json, const std::string& key) {
    size_t pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return 0.0f;
    
    pos = json.find(":", pos);
    if (pos == std::string::npos) return 0.0f;
    
    pos++; // Skip ':'
    while (pos < json.length() && (json[pos] == ' ' || json[pos] == '"')) pos++;
    
    size_t end = pos;
    while (end < json.length() && (isdigit(json[end]) || json[end] == '.' || json[end] == '-' || json[end] == 'e' || json[end] == 'E' || json[end] == '+')) end++;
    
    if (end > pos) {
        try {
            return std::stof(json.substr(pos, end - pos));
        } catch (...) {}
    }
    return 0.0f;
}

// Parse JSON sensor data from SensorServer
// Format: {"type":"android.sensor.accelerometer","values":[x,y,z],...}
// or for values object: {"type":"...","values":{"x":...,"y":...,"z":...}}
bool parseSensorData(const std::string& data, ImuMeasurement& imu) {
    // Check sensor type
    bool isAccel = data.find("accelerometer") != std::string::npos;
    bool isGyro = data.find("gyroscope") != std::string::npos;
    
    if (!isAccel && !isGyro) return false;
    
    float x = 0, y = 0, z = 0;
    
    // Try to parse "values":[x,y,z] array format
    size_t valuesPos = data.find("\"values\"");
    if (valuesPos != std::string::npos) {
        size_t bracketPos = data.find("[", valuesPos);
        if (bracketPos != std::string::npos) {
            // Array format: [x, y, z]
            size_t start = bracketPos + 1;
            size_t end = data.find("]", start);
            if (end != std::string::npos) {
                std::string valuesStr = data.substr(start, end - start);
                // Parse comma-separated values
                std::stringstream ss(valuesStr);
                std::string token;
                int idx = 0;
                while (std::getline(ss, token, ',') && idx < 3) {
                    try {
                        float val = std::stof(token);
                        if (idx == 0) x = val;
                        else if (idx == 1) y = val;
                        else if (idx == 2) z = val;
                    } catch (...) {}
                    idx++;
                }
            }
        } else {
            // Object format: {"x":...,"y":...,"z":...}
            x = extractJsonFloat(data, "x");
            y = extractJsonFloat(data, "y");
            z = extractJsonFloat(data, "z");
        }
    }
    
    double now = std::chrono::duration<double>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    
    // Accumulate sensor data
    std::lock_guard<std::mutex> lock(g_accum_mutex);
    
    if (isAccel) {
        g_sensor_accum.ax = x;
        g_sensor_accum.ay = y;
        g_sensor_accum.az = z;
        g_sensor_accum.hasAccel = true;
        g_sensor_accum.lastAccelTime = now;
    }
    
    if (isGyro) {
        g_sensor_accum.gx = x;
        g_sensor_accum.gy = y;
        g_sensor_accum.gz = z;
        g_sensor_accum.hasGyro = true;
        g_sensor_accum.lastGyroTime = now;
    }
    
    // Only create IMU measurement when we have both sensors
    if (g_sensor_accum.hasAccel && g_sensor_accum.hasGyro) {
        imu.ax = g_sensor_accum.ax;
        imu.ay = g_sensor_accum.ay;
        imu.az = g_sensor_accum.az;
        imu.gx = g_sensor_accum.gx;
        imu.gy = g_sensor_accum.gy;
        imu.gz = g_sensor_accum.gz;
        imu.timestamp = now;
        
        // Reset flags for next reading
        g_sensor_accum.hasAccel = false;
        g_sensor_accum.hasGyro = false;
        
        return true;
    }
    
    return false;
}

// WebSocket frame decoder
std::string decodeWebSocketFrame(const unsigned char* data, size_t len, size_t& consumed) {
    consumed = 0;
    if (len < 2) return "";
    
    bool fin = (data[0] & 0x80) != 0;
    int opcode = data[0] & 0x0F;
    bool masked = (data[1] & 0x80) != 0;
    uint64_t payload_len = data[1] & 0x7F;
    
    size_t header_len = 2;
    
    if (payload_len == 126) {
        if (len < 4) return "";
        payload_len = (data[2] << 8) | data[3];
        header_len = 4;
    } else if (payload_len == 127) {
        if (len < 10) return "";
        payload_len = 0;
        for (int i = 0; i < 8; i++) {
            payload_len = (payload_len << 8) | data[2 + i];
        }
        header_len = 10;
    }
    
    size_t mask_offset = header_len;
    if (masked) header_len += 4;
    
    if (len < header_len + payload_len) return "";
    
    std::string result;
    result.resize(payload_len);
    
    if (masked) {
        const unsigned char* mask = data + mask_offset;
        for (size_t i = 0; i < payload_len; i++) {
            result[i] = data[header_len + i] ^ mask[i % 4];
        }
    } else {
        memcpy(&result[0], data + header_len, payload_len);
    }
    
    consumed = header_len + payload_len;
    
    // Handle text frames (opcode 1)
    if (opcode == 1 || opcode == 2) {
        return result;
    }
    
    return "";
}

// WebSocket IMU receiver thread
// Uses SensorServer API: https://github.com/UmerCodez/SensorServer
void websocketReceiverThread(const std::string& host, int port) {
    std::cout << "Connecting to SensorServer WebSocket at " << host << ":" << port << std::endl;
    
    while (g_running) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) {
            std::cerr << "Failed to create socket" << std::endl;
            std::this_thread::sleep_for(std::chrono::seconds(1));
            continue;
        }
        
        struct hostent* server = gethostbyname(host.c_str());
        if (!server) {
            std::cerr << "Failed to resolve host: " << host << std::endl;
            close(sock);
            std::this_thread::sleep_for(std::chrono::seconds(1));
            continue;
        }
        
        struct sockaddr_in addr;
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);
        memcpy(&addr.sin_addr.s_addr, server->h_addr, server->h_length);
        
        if (connect(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
            std::cerr << "Failed to connect to WebSocket server" << std::endl;
            close(sock);
            std::this_thread::sleep_for(std::chrono::seconds(1));
            continue;
        }
        
        // WebSocket handshake - connect to both accelerometer and gyroscope
        // SensorServer API: /sensors/connect?types=["android.sensor.accelerometer","android.sensor.gyroscope"]
        std::string wsKey = generateWebSocketKey();
        std::string path = "/sensors/connect?types=[\"android.sensor.accelerometer\",\"android.sensor.gyroscope\"]";
        
        std::stringstream request;
        request << "GET " << path << " HTTP/1.1\r\n"
                << "Host: " << host << ":" << port << "\r\n"
                << "Upgrade: websocket\r\n"
                << "Connection: Upgrade\r\n"
                << "Sec-WebSocket-Key: " << wsKey << "\r\n"
                << "Sec-WebSocket-Version: 13\r\n"
                << "\r\n";
        
        std::string req = request.str();
        if (send(sock, req.c_str(), req.size(), 0) < 0) {
            std::cerr << "Failed to send handshake" << std::endl;
            close(sock);
            continue;
        }
        
        // Read handshake response
        char buffer[4096];
        int n = recv(sock, buffer, sizeof(buffer) - 1, 0);
        if (n <= 0 || strstr(buffer, "101") == nullptr) {
            std::cerr << "WebSocket handshake failed" << std::endl;
            close(sock);
            std::this_thread::sleep_for(std::chrono::seconds(1));
            continue;
        }
        
        std::cout << "WebSocket connected to Sensor Server!" << std::endl;
        
        // Set non-blocking with timeout
        struct timeval tv;
        tv.tv_sec = 1;
        tv.tv_usec = 0;
        setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        
        std::vector<unsigned char> recvBuffer;
        int success_count = 0;
        
        while (g_running) {
            n = recv(sock, buffer, sizeof(buffer), 0);
            if (n > 0) {
                recvBuffer.insert(recvBuffer.end(), buffer, buffer + n);
                
                // Process complete frames
                while (recvBuffer.size() >= 2) {
                    size_t consumed = 0;
                    std::string payload = decodeWebSocketFrame(recvBuffer.data(), recvBuffer.size(), consumed);
                    
                    if (consumed == 0) break;  // Incomplete frame
                    
                    recvBuffer.erase(recvBuffer.begin(), recvBuffer.begin() + consumed);
                    
                    if (!payload.empty()) {
                        ImuMeasurement imu;
                        imu.timestamp = std::chrono::duration<double>(
                            std::chrono::system_clock::now().time_since_epoch()).count();
                        
                        if (parseSensorData(payload, imu)) {
                            std::lock_guard<std::mutex> lock(g_imu_mutex);
                            g_imu_buffer.push_back(imu);
                            success_count++;
                            
                            if (g_imu_buffer.size() > 10000) {
                                g_imu_buffer.erase(g_imu_buffer.begin(), g_imu_buffer.begin() + 5000);
                            }
                        }
                    }
                }
                
                if (success_count > 0 && success_count % 500 == 0) {
                    std::cout << "WebSocket IMU: " << success_count << " readings received" << std::endl;
                }
            } else if (n == 0) {
                std::cout << "WebSocket disconnected" << std::endl;
                break;
            }
        }
        
        close(sock);
        
        if (g_running) {
            std::cout << "Reconnecting to WebSocket..." << std::endl;
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }
    }
}

// Get recent IMU measurements and clear buffer
// Simplified approach: just take all buffered measurements since last call
std::vector<ORB_SLAM3::IMU::Point> getImuMeasurements(double t_start, double t_end) {
    std::vector<ORB_SLAM3::IMU::Point> measurements;
    
    std::lock_guard<std::mutex> lock(g_imu_mutex);
    
    // Take all measurements from buffer (they're already accumulated since last frame)
    for (const auto& imu : g_imu_buffer) {
        ORB_SLAM3::IMU::Point point(
            imu.ax, imu.ay, imu.az,
            imu.gx, imu.gy, imu.gz,
            imu.timestamp
        );
        measurements.push_back(point);
    }
    
    // Clear the buffer after taking measurements
    g_imu_buffer.clear();
    
    return measurements;
}

void printUsage(const char* prog) {
    std::cout << "Usage: " << prog << " <vocabulary> <settings> <camera_url> <ws_host> <ws_port>" << std::endl;
    std::cout << std::endl;
    std::cout << "Arguments:" << std::endl;
    std::cout << "  vocabulary   Path to ORB vocabulary file" << std::endl;
    std::cout << "  settings     Path to camera/IMU settings YAML file" << std::endl;
    std::cout << "  camera_url   IP Webcam URL (e.g., http://192.168.1.100:8085/video)" << std::endl;
    std::cout << "  ws_host      Sensor Server WebSocket host (e.g., 192.168.1.100)" << std::endl;
    std::cout << "  ws_port      Sensor Server WebSocket port (e.g., 8081)" << std::endl;
}

int main(int argc, char** argv) {
    if (argc < 6) {
        printUsage(argv[0]);
        return 1;
    }
    
    std::string vocab_path = argv[1];
    std::string settings_path = argv[2];
    std::string video_url = argv[3];
    std::string ws_host = argv[4];
    int ws_port = std::atoi(argv[5]);
    
    std::cout << "=== Phone SLAM (WebSocket Version) ===" << std::endl;
    std::cout << "Vocabulary: " << vocab_path << std::endl;
    std::cout << "Settings: " << settings_path << std::endl;
    std::cout << "Camera URL: " << video_url << std::endl;
    std::cout << "WebSocket: " << ws_host << ":" << ws_port << std::endl;
    std::cout << std::endl;
    
    // Initialize ORB-SLAM3
    std::cout << "Initializing ORB-SLAM3..." << std::endl;
    ORB_SLAM3::System SLAM(vocab_path, settings_path, ORB_SLAM3::System::IMU_MONOCULAR, true);
    
    // Start WebSocket receiver thread
    std::thread ws_thread(websocketReceiverThread, ws_host, ws_port);
    
    // Wait a moment for WebSocket to connect
    std::this_thread::sleep_for(std::chrono::seconds(2));
    
    // Open video stream
    std::cout << "Connecting to IP Webcam..." << std::endl;
    cv::VideoCapture cap(video_url);
    
    if (!cap.isOpened()) {
        std::cerr << "Failed to open camera: " << video_url << std::endl;
        g_running = false;
        ws_thread.join();
        SLAM.Shutdown();
        return 1;
    }
    
    std::cout << "Camera connected!" << std::endl;
    std::cout << "Press Ctrl+C to stop" << std::endl;
    
    double last_frame_time = 0;
    int frame_count = 0;
    auto start_time = std::chrono::steady_clock::now();
    
    cv::Mat frame, gray;
    
    while (g_running) {
        if (!cap.read(frame)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            continue;
        }
        
        // Debug: show frame info once
        static bool shown_info = false;
        if (!shown_info) {
            std::cout << "Frame size: " << frame.cols << "x" << frame.rows 
                      << ", channels: " << frame.channels() << std::endl;
            shown_info = true;
        }
        
        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);
        
        // Debug: show camera feed in separate window
        cv::imshow("Phone Camera", gray);
        
        double timestamp = std::chrono::duration<double>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        
        std::vector<ORB_SLAM3::IMU::Point> imu_measurements;
        if (last_frame_time > 0) {
            imu_measurements = getImuMeasurements(last_frame_time, timestamp);
        }
        
        // Always call TrackMonocular - pass IMU data if we have it
        SLAM.TrackMonocular(gray, timestamp, imu_measurements);
        
        // Debug: print IMU count occasionally
        static int imu_debug_count = 0;
        if (++imu_debug_count % 100 == 0) {
            std::cout << "IMU measurements for this frame: " << imu_measurements.size() << std::endl;
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
    ws_thread.join();
    cap.release();
    
    SLAM.Shutdown();
    
    SLAM.SaveTrajectoryTUM("trajectory_phone.txt");
    SLAM.SaveKeyFrameTrajectoryTUM("keyframe_trajectory_phone.txt");
    
    std::cout << "Done!" << std::endl;
    
    return 0;
}

