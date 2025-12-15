"use client";

import { useState, useEffect } from "react";
import PhoneFrame from "@/components/PhoneFrame";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import ProgressBar from "@/components/ProgressBar";

const scanHistory = [
  { id: 1, name: "Floor 1 - Main Lobby", date: "Today, 2:30 PM", status: "complete" },
  { id: 2, name: "Floor 2 - East Wing", date: "Today, 11:45 AM", status: "complete" },
  { id: 3, name: "Floor 2 - West Wing", date: "Yesterday", status: "processing" },
];

export default function AdminDashboardPage() {
  const [lidarProgress, setLidarProgress] = useState(67);
  const [uploadProgress, setUploadProgress] = useState(45);
  const [processingProgress, setProcessingProgress] = useState(23);
  const [isScanning, setIsScanning] = useState(true);

  // Simulate progress updates
  useEffect(() => {
    if (!isScanning) return;

    const interval = setInterval(() => {
      setLidarProgress((prev) => Math.min(prev + Math.random() * 2, 100));
      setUploadProgress((prev) => Math.min(prev + Math.random() * 1.5, 100));
      setProcessingProgress((prev) => Math.min(prev + Math.random() * 1, 100));
    }, 1000);

    return () => clearInterval(interval);
  }, [isScanning]);

  const AdminBadge = () => (
    <div className="px-3 py-1 bg-secondary/20 border border-secondary/50 rounded-full">
      <span className="text-xs font-semibold text-secondary">ADMIN</span>
    </div>
  );

  return (
    <PhoneFrame>
      <div className="relative w-full h-full bg-background overflow-hidden">
        {/* Header */}
        <Header
          title="Scanning Dashboard"
          showBack
          backHref="/"
          variant="admin"
          rightElement={<AdminBadge />}
        />

        {/* Scrollable content */}
        <div className="absolute top-16 bottom-24 left-0 right-0 overflow-y-auto px-4 pt-14">
          <div className="animate-fade-in space-y-5 pb-4">
            {/* LiDAR Scanner Status */}
            <section className="glass rounded-2xl p-4" aria-labelledby="lidar-heading">
              <div className="flex items-center justify-between mb-4">
                <h2 id="lidar-heading" className="text-lg font-semibold text-foreground">
                  LiDAR Scanner
                </h2>
                <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
                  isScanning ? "bg-success/20 text-success" : "bg-foreground-muted/20 text-foreground-muted"
                }`}>
                  <div className={`w-2 h-2 rounded-full ${isScanning ? "bg-success animate-pulse" : "bg-foreground-muted"}`} />
                  <span className="text-xs font-medium">
                    {isScanning ? "Active" : "Idle"}
                  </span>
                </div>
              </div>

              {/* 3D LiDAR Point Cloud Visualization */}
              <div className="relative w-full aspect-[4/3] rounded-xl overflow-hidden mb-4">
                {/* Real 3D point cloud scan background - indoor corridor/hallway */}
                <div 
                  className="absolute inset-0 bg-cover bg-center"
                  style={{
                    backgroundImage: `url('https://images.unsplash.com/photo-1497366216548-37526070297c?w=800&q=80')`,
                    filter: 'hue-rotate(160deg) saturate(0.4) brightness(0.8)',
                  }}
                />
                
                {/* Dark overlay for visibility */}
                <div className="absolute inset-0 bg-gradient-to-t from-background via-background/60 to-background/40" />
                
                {/* Scanning overlay effects */}
                {isScanning && (
                  <>
                    {/* Horizontal scan line */}
                    <div className="absolute inset-0 overflow-hidden">
                      <div 
                        className="absolute left-0 right-0 h-1 bg-gradient-to-r from-transparent via-primary to-transparent opacity-80 animate-scan-line"
                        style={{ boxShadow: '0 0 20px var(--primary), 0 0 40px var(--primary)' }}
                      />
                    </div>
                    
                    {/* Point cloud particles appearing */}
                    <div className="absolute inset-0">
                      <div className="absolute top-[15%] left-[20%] w-1 h-1 bg-primary rounded-full animate-pulse shadow-[0_0_6px_var(--primary)]" />
                      <div className="absolute top-[25%] left-[65%] w-1.5 h-1.5 bg-primary rounded-full animate-pulse shadow-[0_0_8px_var(--primary)]" style={{ animationDelay: '0.2s' }} />
                      <div className="absolute top-[40%] left-[35%] w-1 h-1 bg-secondary rounded-full animate-pulse shadow-[0_0_6px_var(--secondary)]" style={{ animationDelay: '0.4s' }} />
                      <div className="absolute top-[55%] left-[75%] w-1.5 h-1.5 bg-primary rounded-full animate-pulse shadow-[0_0_8px_var(--primary)]" style={{ animationDelay: '0.6s' }} />
                      <div className="absolute top-[70%] left-[25%] w-1 h-1 bg-primary rounded-full animate-pulse shadow-[0_0_6px_var(--primary)]" style={{ animationDelay: '0.8s' }} />
                      <div className="absolute top-[60%] left-[55%] w-1.5 h-1.5 bg-secondary rounded-full animate-pulse shadow-[0_0_8px_var(--secondary)]" style={{ animationDelay: '1s' }} />
                      <div className="absolute top-[30%] left-[80%] w-1 h-1 bg-primary rounded-full animate-pulse shadow-[0_0_6px_var(--primary)]" style={{ animationDelay: '0.3s' }} />
                      <div className="absolute top-[75%] left-[45%] w-1 h-1 bg-primary rounded-full animate-pulse shadow-[0_0_6px_var(--primary)]" style={{ animationDelay: '0.7s' }} />
                    </div>
                    
                    {/* Grid overlay for 3D effect */}
                    <div 
                      className="absolute inset-0 opacity-20"
                      style={{
                        backgroundImage: `
                          linear-gradient(rgba(0, 212, 255, 0.3) 1px, transparent 1px),
                          linear-gradient(90deg, rgba(0, 212, 255, 0.3) 1px, transparent 1px)
                        `,
                        backgroundSize: '20px 20px',
                      }}
                    />
                  </>
                )}
                
                {/* Corner brackets for scan area */}
                <div className="absolute top-2 left-2 w-6 h-6 border-t-2 border-l-2 border-primary rounded-tl" />
                <div className="absolute top-2 right-2 w-6 h-6 border-t-2 border-r-2 border-primary rounded-tr" />
                <div className="absolute bottom-2 left-2 w-6 h-6 border-b-2 border-l-2 border-primary rounded-bl" />
                <div className="absolute bottom-2 right-2 w-6 h-6 border-b-2 border-r-2 border-primary rounded-br" />
                
                {/* Status overlay */}
                <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between">
                  <div className="px-2 py-1 bg-background/80 rounded text-xs font-mono text-primary">
                    3D SCAN
                  </div>
                  <div className="px-2 py-1 bg-background/80 rounded text-xs font-mono text-foreground-muted">
                    {Math.floor(lidarProgress)}% captured
                  </div>
                </div>
              </div>

              <ProgressBar
                value={lidarProgress}
                label="Scan Progress"
                variant="primary"
                size="md"
              />
              
              <div className="mt-3 flex justify-between text-sm text-foreground-muted">
                <span>Points captured: {Math.floor(lidarProgress * 1247)}</span>
                <span>~{Math.ceil((100 - lidarProgress) / 3)} min left</span>
              </div>
            </section>

            {/* Upload Status */}
            <section className="glass rounded-2xl p-4" aria-labelledby="upload-heading">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-secondary/20 flex items-center justify-center">
                  <svg
                    width="22"
                    height="22"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    className="text-secondary"
                  >
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                    <polyline points="17 8 12 3 7 8" />
                    <line x1="12" y1="3" x2="12" y2="15" />
                  </svg>
                </div>
                <h2 id="upload-heading" className="text-lg font-semibold text-foreground">
                  Upload to Cloud
                </h2>
              </div>
              
              <ProgressBar
                value={uploadProgress}
                variant="secondary"
                size="md"
              />
              
              <p className="mt-2 text-sm text-foreground-muted">
                Uploading scan data... {Math.floor(uploadProgress * 0.47)}MB / 47MB
              </p>
            </section>

            {/* Map Processing */}
            <section className="glass rounded-2xl p-4" aria-labelledby="processing-heading">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-warning/20 flex items-center justify-center">
                  <svg
                    width="22"
                    height="22"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    className="text-warning"
                  >
                    <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
                  </svg>
                </div>
                <h2 id="processing-heading" className="text-lg font-semibold text-foreground">
                  Map Processing
                </h2>
              </div>

              <ProgressBar
                value={processingProgress}
                variant="warning"
                size="md"
              />

              {/* Processing stages */}
              <div className="mt-4 space-y-2">
                {[
                  { name: "Point Cloud Generation", done: processingProgress > 20 },
                  { name: "Surface Reconstruction", done: processingProgress > 45 },
                  { name: "Feature Detection", done: processingProgress > 70 },
                  { name: "Navigation Mesh", done: processingProgress > 90 },
                ].map((stage, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <div className={`w-5 h-5 rounded-full flex items-center justify-center ${
                      stage.done ? "bg-success" : "bg-background-card"
                    }`}>
                      {stage.done ? (
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3">
                          <path d="M20 6L9 17l-5-5" />
                        </svg>
                      ) : (
                        <div className="w-2 h-2 rounded-full bg-foreground-muted/50" />
                      )}
                    </div>
                    <span className={`text-sm ${stage.done ? "text-foreground" : "text-foreground-muted"}`}>
                      {stage.name}
                    </span>
                  </div>
                ))}
              </div>
            </section>

            {/* Scan History */}
            <section aria-labelledby="history-heading">
              <h2 id="history-heading" className="text-sm font-semibold text-foreground-muted uppercase tracking-wide mb-3">
                Recent Scans
              </h2>
              <div className="space-y-2">
                {scanHistory.map((scan) => (
                  <div
                    key={scan.id}
                    className="flex items-center gap-3 p-4 bg-background-card rounded-xl"
                  >
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                      scan.status === "complete" ? "bg-success/20" : "bg-warning/20"
                    }`}>
                      {scan.status === "complete" ? (
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-success">
                          <path d="M20 6L9 17l-5-5" />
                        </svg>
                      ) : (
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-warning animate-spin">
                          <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4" />
                        </svg>
                      )}
                    </div>
                    <div className="flex-1">
                      <p className="font-medium text-foreground">{scan.name}</p>
                      <p className="text-sm text-foreground-muted">{scan.date}</p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </div>

        {/* Footer */}
        <Footer>
          <div className="flex gap-3">
            <button
              onClick={() => setIsScanning(!isScanning)}
              className={`flex-1 h-14 font-semibold text-lg rounded-2xl transition-colors flex items-center justify-center gap-2 ${
                isScanning
                  ? "bg-error/20 border-2 border-error/50 text-error hover:bg-error/30"
                  : "bg-primary text-background hover:bg-primary-dark"
              }`}
            >
              {isScanning ? (
                <>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                    <rect x="6" y="4" width="4" height="16" rx="1" />
                    <rect x="14" y="4" width="4" height="16" rx="1" />
                  </svg>
                  Pause Scan
                </>
              ) : (
                <>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                    <polygon points="5 3 19 12 5 21 5 3" />
                  </svg>
                  Start Scan
                </>
              )}
            </button>
            <button
              className="w-14 h-14 bg-background-card border-2 border-primary/30 rounded-2xl flex items-center justify-center text-primary hover:bg-primary/10 transition-colors"
              aria-label="View maps"
            >
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
                <line x1="8" y1="2" x2="8" y2="18" />
                <line x1="16" y1="6" x2="16" y2="22" />
              </svg>
            </button>
          </div>
        </Footer>
      </div>
    </PhoneFrame>
  );
}

