"use client";

import { useRouter } from "next/navigation";
import PhoneFrame from "@/components/PhoneFrame";
import QRScanner from "@/components/QRScanner";

export default function VisitorInitPage() {
  const router = useRouter();

  const handleScan = () => {
    // Simulate successful scan - navigate to destination search
    router.push("/visitor/search");
  };

  return (
    <PhoneFrame>
      <div className="relative w-full h-full overflow-hidden">
        {/* Simulated camera background */}
        <div className="absolute inset-0 camera-bg">
          {/* Scan line effect */}
          <div className="absolute inset-0 overflow-hidden pointer-events-none">
            <div className="absolute left-0 right-0 h-32 bg-gradient-to-b from-primary/10 to-transparent animate-scan-line" />
          </div>
          
          {/* Noise/grain overlay */}
          <div 
            className="absolute inset-0 opacity-20 pointer-events-none"
            style={{
              backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E")`,
            }}
          />
        </div>

        {/* Top gradient overlay */}
        <div className="absolute top-0 left-0 right-0 h-32 bg-gradient-to-b from-background/80 to-transparent z-10" />

        {/* Header content */}
        <div className="relative z-20 pt-20 px-6 text-center">
          {/* Logo */}
          <div className="flex items-center justify-center gap-3 mb-2">
            <div className="w-10 h-10 rounded-xl bg-primary/20 border border-primary/50 flex items-center justify-center">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                className="text-primary"
              >
                <polygon points="3 11 22 2 13 21 11 13 3 11" />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-foreground">Vectura AI</h1>
          </div>
          <p className="text-sm text-foreground-muted">Indoor AR Navigation</p>
        </div>

        {/* Main scanner area */}
        <div className="absolute inset-0 flex items-center justify-center z-20">
          <QRScanner onScan={handleScan} />
        </div>

        {/* Bottom gradient overlay */}
        <div className="absolute bottom-0 left-0 right-0 h-48 bg-gradient-to-t from-background via-background/80 to-transparent z-10" />

        {/* Bottom info */}
        <div className="absolute bottom-24 left-0 right-0 z-20 px-6">
          <div className="glass rounded-2xl p-4 text-center">
            <div className="flex items-center justify-center gap-2 text-primary mb-2">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="12" cy="12" r="10" />
                <path d="M12 16v-4M12 8h.01" />
              </svg>
              <span className="font-medium">First Time Here?</span>
            </div>
            <p className="text-sm text-foreground-muted">
              Look for QR codes near building entrances or information desks
            </p>
          </div>
        </div>
      </div>
    </PhoneFrame>
  );
}

