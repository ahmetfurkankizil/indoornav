"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import PhoneFrame from "@/components/PhoneFrame";
import Footer from "@/components/Footer";
import { ARArrow, ARWaypoint, DirectionIndicator } from "@/components/ARElements";

export default function VisitorNavigatePage() {
  const router = useRouter();
  const [distance, setDistance] = useState(47);
  const [currentInstruction, setCurrentInstruction] = useState<{
    text: string;
    distance: string;
    icon: "straight" | "left" | "right" | "destination";
  }>({
    text: "Continue straight",
    distance: "45m",
    icon: "straight",
  });

  // Simulate distance countdown
  useEffect(() => {
    const interval = setInterval(() => {
      setDistance((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 2000);

    return () => clearInterval(interval);
  }, []);

  // Update instructions based on distance
  useEffect(() => {
    if (distance <= 5) {
      setCurrentInstruction({
        text: "Destination on right",
        distance: `${distance}m`,
        icon: "destination",
      });
    } else if (distance <= 20) {
      setCurrentInstruction({
        text: "Turn right ahead",
        distance: `${distance}m`,
        icon: "right",
      });
    } else if (distance <= 35) {
      setCurrentInstruction({
        text: "Turn left at hallway",
        distance: `${distance}m`,
        icon: "left",
      });
    } else {
      setCurrentInstruction({
        text: "Continue straight",
        distance: `${distance}m`,
        icon: "straight",
      });
    }
  }, [distance]);

  const handleEndNavigation = () => {
    router.push("/visitor/search");
  };

  const formatTime = (meters: number) => {
    const minutes = Math.ceil(meters / 50); // Assume ~50m per minute walking
    return `${minutes} min`;
  };

  return (
    <PhoneFrame>
      <div className="relative w-full h-full overflow-hidden">
        {/* AR Camera Background - Real-life hallway image */}
        <div className="absolute inset-0">
          {/* Background image of indoor corridor */}
          <div 
            className="absolute inset-0 bg-cover bg-center bg-no-repeat"
            style={{
              backgroundImage: `url('https://images.unsplash.com/photo-1497366216548-37526070297c?w=800&q=80')`,
            }}
          />
          
          {/* Slight dark overlay for better AR element visibility */}
          <div className="absolute inset-0 bg-black/30" />
          
          {/* Subtle scan line effect for AR feel */}
          <div className="absolute inset-0 pointer-events-none opacity-10">
            <div 
              className="w-full h-full"
              style={{
                backgroundImage: `repeating-linear-gradient(
                  0deg,
                  transparent,
                  transparent 2px,
                  rgba(0, 212, 255, 0.03) 2px,
                  rgba(0, 212, 255, 0.03) 4px
                )`,
              }}
            />
          </div>
        </div>

        {/* Top overlay gradient */}
        <div className="absolute top-0 left-0 right-0 h-40 bg-gradient-to-b from-background/90 via-background/50 to-transparent z-10" />

        {/* Direction Indicator (Top) */}
        <div className="absolute top-16 left-4 right-4 z-30 pt-6">
          <DirectionIndicator
            instruction={currentInstruction.text}
            distance={currentInstruction.distance}
            icon={currentInstruction.icon}
          />
        </div>

        {/* AR Elements */}
        <div className="absolute inset-0 z-20 pointer-events-none" style={{ perspective: "800px" }}>
          {/* Main navigation arrow */}
          <ARArrow
            direction={distance <= 20 ? "right" : "forward"}
            distance={distance}
            style={{
              left: "50%",
              bottom: "35%",
              transform: "translateX(-50%)",
            }}
          />

          {/* Waypoints along the path */}
          <ARWaypoint
            style={{
              left: "50%",
              bottom: "55%",
            }}
          />
          
          {distance <= 30 && (
            <ARWaypoint
              label="Turn Point"
              distance="20m"
              style={{
                left: "70%",
                bottom: "45%",
              }}
            />
          )}

          {distance <= 15 && (
            <ARWaypoint
              label="Room 203"
              isDestination
              style={{
                left: "75%",
                bottom: "50%",
              }}
            />
          )}
        </div>

        {/* Distance indicator floating */}
        <div className="absolute left-1/2 -translate-x-1/2 bottom-40 z-30">
          <div className="glass rounded-full px-6 py-3 flex items-center gap-3">
            <div className="w-3 h-3 rounded-full bg-primary animate-pulse" />
            <span className="text-2xl font-bold text-foreground font-mono">{distance}m</span>
            <span className="text-foreground-muted">remaining</span>
          </div>
        </div>

        {/* Bottom gradient */}
        <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-background to-transparent z-10" />

        {/* Footer */}
        <div className="absolute bottom-0 left-0 right-0 z-40">
          <Footer variant="navigation">
            <div className="space-y-3">
              {/* Progress bar */}
              <div className="h-1 bg-background-card rounded-full overflow-hidden">
                <div 
                  className="h-full bg-primary rounded-full transition-all duration-1000 ease-out"
                  style={{ width: `${100 - (distance / 47) * 100}%` }}
                />
              </div>
              
              {/* Info row */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xl font-bold text-foreground">Room 203</p>
                  <p className="text-sm text-foreground-muted">
                    {distance}m • {formatTime(distance)} remaining
                  </p>
                </div>
                
                <button
                  onClick={handleEndNavigation}
                  className="px-6 py-3 bg-error/20 border border-error/50 text-error font-medium rounded-xl hover:bg-error/30 transition-colors"
                  aria-label="End navigation"
                >
                  End
                </button>
              </div>
            </div>
          </Footer>
        </div>

        {/* Recenter button */}
        <button
          className="absolute right-4 bottom-44 z-30 w-12 h-12 glass rounded-full flex items-center justify-center text-primary hover:bg-primary/20 transition-colors"
          aria-label="Recenter view"
        >
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <circle cx="12" cy="12" r="3" />
            <path d="M12 2v4M12 18v4M2 12h4M18 12h4" />
          </svg>
        </button>
      </div>
    </PhoneFrame>
  );
}

