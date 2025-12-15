/**
 * ORB-SLAM3 Phone Demo - Monocular Phone Camera Interface
 * 
 * This program reads video frames from an IP Webcam stream (Android phone)
 * and processes them with ORB-SLAM3 monocular SLAM.
 * 
 * Usage:
 *   ./phone_mono <path_to_vocabulary> <path_to_settings> <stream_url>
 * 
 * Example:
 *   ./phone_mono ../Vocabulary/ORBvoc.txt phone_camera.yaml http://192.168.1.100:8080/video
 */

#include <iostream>
#include <algorithm>
#include <fstream>
#include <chrono>
#include <iomanip>
#include <csignal>

#include <opencv2/core.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>

#include "System.h"

using namespace std;

// Global flag for graceful shutdown
volatile sig_atomic_t shutdown_requested = 0;

void signal_handler(int signal) {
    cout << "\n[INFO] Shutdown requested (signal " << signal << ")" << endl;
    shutdown_requested = 1;
}

void print_usage(const char* program_name) {
    cout << endl;
    cout << "ORB-SLAM3 Phone Demo - Monocular Mode" << endl;
    cout << "======================================" << endl;
    cout << endl;
    cout << "Usage: " << program_name << " <vocabulary> <settings> <stream_url>" << endl;
    cout << endl;
    cout << "Arguments:" << endl;
    cout << "  vocabulary   Path to ORB vocabulary file (ORBvoc.txt)" << endl;
    cout << "  settings     Path to camera settings YAML file" << endl;
    cout << "  stream_url   IP Webcam video stream URL" << endl;
    cout << endl;
    cout << "Example:" << endl;
    cout << "  " << program_name << " ../Vocabulary/ORBvoc.txt phone_camera.yaml http://192.168.1.100:8080/video" << endl;
    cout << endl;
    cout << "Controls:" << endl;
    cout << "  ESC or 'q'  - Quit the application" << endl;
    cout << "  SPACE       - Pause/resume tracking" << endl;
    cout << "  's'         - Save current map" << endl;
    cout << "  'r'         - Reset SLAM system" << endl;
    cout << endl;
}

class FrameGrabber {
public:
    FrameGrabber(const string& stream_url) : url_(stream_url), connected_(false) {
        connect();
    }

    bool connect() {
        cout << "[INFO] Connecting to stream: " << url_ << endl;
        
        // Try to open the video stream
        cap_.open(url_, cv::CAP_FFMPEG);
        
        if (!cap_.isOpened()) {
            // Try with different backend
            cap_.open(url_, cv::CAP_ANY);
        }
        
        if (!cap_.isOpened()) {
            cerr << "[ERROR] Could not connect to video stream!" << endl;
            cerr << "[ERROR] Make sure IP Webcam is running on your phone." << endl;
            connected_ = false;
            return false;
        }
        
        // Get stream properties
        width_ = static_cast<int>(cap_.get(cv::CAP_PROP_FRAME_WIDTH));
        height_ = static_cast<int>(cap_.get(cv::CAP_PROP_FRAME_HEIGHT));
        fps_ = cap_.get(cv::CAP_PROP_FPS);
        
        if (fps_ <= 0) fps_ = 30.0;  // Default to 30 FPS if not available
        
        cout << "[INFO] Connected! Resolution: " << width_ << "x" << height_ 
             << " @ " << fps_ << " FPS" << endl;
        
        connected_ = true;
        return true;
    }

    bool grab(cv::Mat& frame) {
        if (!connected_) {
            if (!connect()) return false;
        }
        
        if (!cap_.read(frame)) {
            cerr << "[WARN] Failed to grab frame, attempting reconnect..." << endl;
            connected_ = false;
            return false;
        }
        
        return true;
    }

    bool isConnected() const { return connected_; }
    int width() const { return width_; }
    int height() const { return height_; }
    double fps() const { return fps_; }

private:
    string url_;
    cv::VideoCapture cap_;
    bool connected_;
    int width_, height_;
    double fps_;
};

int main(int argc, char **argv) {
    // Check arguments
    if (argc != 4) {
        print_usage(argv[0]);
        return 1;
    }

    string vocabulary_path = argv[1];
    string settings_path = argv[2];
    string stream_url = argv[3];

    // Check if files exist
    ifstream vocab_file(vocabulary_path);
    if (!vocab_file.good()) {
        cerr << "[ERROR] Vocabulary file not found: " << vocabulary_path << endl;
        return 1;
    }
    vocab_file.close();

    ifstream settings_file(settings_path);
    if (!settings_file.good()) {
        cerr << "[ERROR] Settings file not found: " << settings_path << endl;
        return 1;
    }
    settings_file.close();

    // Set up signal handlers for graceful shutdown
    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);

    cout << endl;
    cout << "========================================" << endl;
    cout << "ORB-SLAM3 Phone Demo - Starting..." << endl;
    cout << "========================================" << endl;
    cout << endl;

    // Connect to phone camera stream
    FrameGrabber grabber(stream_url);
    if (!grabber.isConnected()) {
        return 1;
    }

    // Create SLAM system (Monocular mode, with viewer)
    cout << endl << "[INFO] Initializing ORB-SLAM3..." << endl;
    ORB_SLAM3::System SLAM(vocabulary_path, settings_path, ORB_SLAM3::System::MONOCULAR, true);

    cout << endl;
    cout << "[INFO] ORB-SLAM3 initialized successfully!" << endl;
    cout << "[INFO] Starting tracking loop..." << endl;
    cout << endl;
    cout << "Controls: ESC/q=quit, SPACE=pause, s=save, r=reset" << endl;
    cout << endl;

    // Tracking variables
    cv::Mat frame;
    bool paused = false;
    int frame_count = 0;
    int tracking_lost_count = 0;
    
    auto start_time = chrono::steady_clock::now();
    auto last_fps_time = start_time;
    int fps_frame_count = 0;
    double current_fps = 0;

    // Main tracking loop
    while (!shutdown_requested) {
        // Grab frame from phone
        if (!grabber.grab(frame)) {
            // Try to reconnect
            this_thread::sleep_for(chrono::milliseconds(1000));
            continue;
        }

        if (frame.empty()) continue;

        // Calculate timestamp in seconds
        auto now = chrono::steady_clock::now();
        double timestamp = chrono::duration<double>(now - start_time).count();

        // Track frame if not paused
        if (!paused) {
            // Pass frame to SLAM system
            Sophus::SE3f pose = SLAM.TrackMonocular(frame, timestamp);

            // Check tracking state
            int state = SLAM.GetTrackingState();
            
            if (state == 2) {  // OK
                tracking_lost_count = 0;
            } else if (state == 3) {  // LOST
                tracking_lost_count++;
                if (tracking_lost_count % 30 == 1) {
                    cout << "[WARN] Tracking lost! Move camera slowly or return to mapped area." << endl;
                }
            }
        }

        // Calculate FPS
        fps_frame_count++;
        auto fps_elapsed = chrono::duration<double>(now - last_fps_time).count();
        if (fps_elapsed >= 1.0) {
            current_fps = fps_frame_count / fps_elapsed;
            fps_frame_count = 0;
            last_fps_time = now;
        }

        // Display frame with overlay
        cv::Mat display;
        frame.copyTo(display);

        // Draw FPS and status
        string fps_text = "FPS: " + to_string(static_cast<int>(current_fps));
        cv::putText(display, fps_text, cv::Point(10, 30), 
                   cv::FONT_HERSHEY_SIMPLEX, 0.8, cv::Scalar(0, 255, 0), 2);

        int state = SLAM.GetTrackingState();
        string state_text;
        cv::Scalar state_color;
        
        switch (state) {
            case 0:
                state_text = "SYSTEM NOT READY";
                state_color = cv::Scalar(0, 165, 255);  // Orange
                break;
            case 1:
                state_text = "NO IMAGES YET";
                state_color = cv::Scalar(0, 165, 255);  // Orange
                break;
            case 2:
                state_text = "TRACKING OK";
                state_color = cv::Scalar(0, 255, 0);    // Green
                break;
            case 3:
                state_text = "TRACKING LOST";
                state_color = cv::Scalar(0, 0, 255);    // Red
                break;
            default:
                state_text = "UNKNOWN STATE";
                state_color = cv::Scalar(128, 128, 128);
                break;
        }

        if (paused) {
            state_text = "PAUSED";
            state_color = cv::Scalar(0, 255, 255);  // Yellow
        }

        cv::putText(display, state_text, cv::Point(10, 60),
                   cv::FONT_HERSHEY_SIMPLEX, 0.8, state_color, 2);

        // Show keypoints count
        int num_matches = SLAM.GetTrackedMapPoints().size();
        string matches_text = "Matches: " + to_string(num_matches);
        cv::putText(display, matches_text, cv::Point(10, 90),
                   cv::FONT_HERSHEY_SIMPLEX, 0.6, cv::Scalar(255, 255, 255), 2);

        // Display
        cv::imshow("ORB-SLAM3 Phone Demo", display);

        // Handle keyboard input
        int key = cv::waitKey(1);
        
        if (key == 27 || key == 'q' || key == 'Q') {  // ESC or 'q'
            cout << "[INFO] Quit requested" << endl;
            break;
        } else if (key == ' ') {  // Space
            paused = !paused;
            cout << "[INFO] " << (paused ? "Paused" : "Resumed") << endl;
        } else if (key == 's' || key == 'S') {  // Save
            cout << "[INFO] Saving trajectory..." << endl;
            SLAM.SaveTrajectoryTUM("phone_trajectory_tum.txt");
            SLAM.SaveKeyFrameTrajectoryTUM("phone_keyframes_tum.txt");
            cout << "[INFO] Trajectory saved to phone_trajectory_tum.txt!" << endl;
        } else if (key == 'r' || key == 'R') {  // Reset
            cout << "[INFO] Resetting SLAM system..." << endl;
            SLAM.Reset();
            cout << "[INFO] Reset complete" << endl;
        }

        frame_count++;
    }

    // Cleanup
    cout << endl;
    cout << "[INFO] Shutting down..." << endl;
    
    cv::destroyAllWindows();
    
    // Stop SLAM and save trajectory
    cout << "[INFO] Saving trajectory..." << endl;
    SLAM.Shutdown();
    
    // Save trajectory files
    SLAM.SaveTrajectoryTUM("phone_trajectory_tum.txt");
    SLAM.SaveKeyFrameTrajectoryTUM("phone_keyframes_tum.txt");
    
    cout << "[INFO] Trajectory saved to phone_trajectory_tum.txt" << endl;
    cout << endl;
    cout << "========================================" << endl;
    cout << "ORB-SLAM3 Phone Demo - Finished" << endl;
    cout << "========================================" << endl;
    cout << "Processed " << frame_count << " frames" << endl;
    cout << endl;

    return 0;
}

