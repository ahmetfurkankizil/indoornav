"use client";

interface QRScannerProps {
  onScan?: () => void;
  instruction?: string;
}

export default function QRScanner({
  onScan,
  instruction = "Scan Building QR Code",
}: QRScannerProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-8">
      {/* QR Scanner frame */}
      <div
        className="relative w-64 h-64"
        role="img"
        aria-label="QR code scanner viewfinder"
      >
        {/* Animated corners */}
        <div className="absolute top-0 left-0 w-12 h-12 border-t-4 border-l-4 border-primary rounded-tl-lg animate-corner-pulse" />
        <div className="absolute top-0 right-0 w-12 h-12 border-t-4 border-r-4 border-primary rounded-tr-lg animate-corner-pulse" style={{ animationDelay: "0.2s" }} />
        <div className="absolute bottom-0 left-0 w-12 h-12 border-b-4 border-l-4 border-primary rounded-bl-lg animate-corner-pulse" style={{ animationDelay: "0.4s" }} />
        <div className="absolute bottom-0 right-0 w-12 h-12 border-b-4 border-r-4 border-primary rounded-br-lg animate-corner-pulse" style={{ animationDelay: "0.6s" }} />

        {/* Scanner line */}
        <div className="absolute inset-4 overflow-hidden">
          <div className="absolute left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-primary to-transparent animate-scanner-line" />
        </div>

        {/* Center QR icon */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-20 h-20 opacity-30">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              className="text-primary"
              aria-hidden="true"
            >
              <rect x="3" y="3" width="7" height="7" rx="1" />
              <rect x="14" y="3" width="7" height="7" rx="1" />
              <rect x="3" y="14" width="7" height="7" rx="1" />
              <rect x="14" y="14" width="4" height="4" rx="0.5" />
              <path d="M21 14v3a2 2 0 01-2 2h-3" />
            </svg>
          </div>
        </div>

        {/* Glow effect */}
        <div className="absolute inset-0 rounded-lg bg-primary/5 animate-pulse-glow pointer-events-none" />
      </div>

      {/* Instruction text */}
      <div className="text-center space-y-2">
        <p className="text-xl font-semibold text-foreground animate-pulse">
          {instruction}
        </p>
        <p className="text-sm text-foreground-muted">
          Point your camera at a building QR code
        </p>
      </div>

      {/* Manual entry button */}
      <button
        onClick={onScan}
        className="mt-4 px-8 py-4 bg-background-card border-2 border-primary/50 rounded-2xl text-primary font-medium text-lg hover:bg-primary/10 hover:border-primary transition-all min-h-[56px]"
        aria-label="Enter building code manually"
      >
        Enter Code Manually
      </button>
    </div>
  );
}

