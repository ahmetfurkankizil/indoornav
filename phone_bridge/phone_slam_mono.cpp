/**
 * Phone SLAM Bridge - Monocular Only (No IMU)
 * 
 * Simpler version without IMU to test camera tracking first.
 */

#include <iostream>
#include <chrono>
#include <opencv2/opencv.hpp>
#include "System.h"

int main(int argc, char** argv) {
    if (argc < 4) {
        std::cout << "Usage: " << argv[0] << " <vocabulary> <settings> <camera_url>" << std::endl;
        return 1;
    }
    
    std::string vocab_path = argv[1];
    std::string settings_path = argv[2];
    std::string video_url = argv[3];
    
    std::cout << "=== Phone SLAM (Monocular Only) ===" << std::endl;
    std::cout << "Vocabulary: " << vocab_path << std::endl;
    std::cout << "Settings: " << settings_path << std::endl;
    std::cout << "Camera URL: " << video_url << std::endl;
    
    // Initialize ORB-SLAM3 in MONOCULAR mode (no IMU)
    std::cout << "Initializing ORB-SLAM3 in Monocular mode..." << std::endl;
    ORB_SLAM3::System SLAM(vocab_path, settings_path, ORB_SLAM3::System::MONOCULAR, true);
    
    std::cout << "Connecting to IP Webcam..." << std::endl;
    cv::VideoCapture cap(video_url);
    
    if (!cap.isOpened()) {
        std::cerr << "Failed to open camera: " << video_url << std::endl;
        SLAM.Shutdown();
        return 1;
    }
    
    std::cout << "Camera connected!" << std::endl;
    std::cout << "Press 'q' to quit" << std::endl;
    
    int frame_count = 0;
    auto start_time = std::chrono::steady_clock::now();
    cv::Mat frame, gray;
    
    while (true) {
        if (!cap.read(frame)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            continue;
        }
        
        static bool shown_info = false;
        if (!shown_info) {
            std::cout << "Frame size: " << frame.cols << "x" << frame.rows << std::endl;
            shown_info = true;
        }
        
        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);
        
        double timestamp = std::chrono::duration<double>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
        
        // Track without IMU
        SLAM.TrackMonocular(gray, timestamp);
        
        frame_count++;
        
        if (frame_count % 100 == 0) {
            auto now = std::chrono::steady_clock::now();
            double elapsed = std::chrono::duration<double>(now - start_time).count();
            std::cout << "Frames: " << frame_count << ", FPS: " << (frame_count / elapsed) << std::endl;
        }
        
        // Show debug window
        cv::imshow("Phone Camera", gray);
        if (cv::waitKey(1) == 'q') break;
    }
    
    std::cout << "Shutting down..." << std::endl;
    cap.release();
    SLAM.Shutdown();
    
    SLAM.SaveTrajectoryTUM("trajectory_mono.txt");
    
    std::cout << "Done!" << std::endl;
    return 0;
}


