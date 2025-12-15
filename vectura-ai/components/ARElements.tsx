"use client";

interface ARArrowProps {
  direction?: "forward" | "left" | "right";
  distance?: number;
  style?: React.CSSProperties;
}

export function ARArrow({ direction = "forward", distance = 5, style }: ARArrowProps) {
  const rotations = {
    forward: "rotate-0",
    left: "-rotate-90",
    right: "rotate-90",
  };

  return (
    <div
      className={`absolute ${rotations[direction]}`}
      style={style}
      role="img"
      aria-label={`Navigate ${direction}, ${distance} meters`}
    >
      {/* 3D Arrow with glow */}
      <div className="relative animate-bounce-arrow" style={{ transformStyle: "preserve-3d" }}>
        {/* Arrow shadow/glow */}
        <div className="absolute inset-0 blur-md bg-primary/50" />
        
        {/* Arrow body */}
        <svg
          width="60"
          height="80"
          viewBox="0 0 60 80"
          fill="none"
          className="relative z-10 drop-shadow-[0_0_15px_var(--primary)]"
        >
          {/* Arrow shape */}
          <path
            d="M30 0 L55 40 L40 40 L40 80 L20 80 L20 40 L5 40 Z"
            fill="url(#arrowGradient)"
            stroke="var(--primary)"
            strokeWidth="2"
          />
          <defs>
            <linearGradient id="arrowGradient" x1="30" y1="0" x2="30" y2="80" gradientUnits="userSpaceOnUse">
              <stop offset="0%" stopColor="var(--primary)" />
              <stop offset="100%" stopColor="var(--primary-dark)" />
            </linearGradient>
          </defs>
        </svg>
      </div>
    </div>
  );
}

interface ARWaypointProps {
  label?: string;
  distance?: string;
  isDestination?: boolean;
  style?: React.CSSProperties;
}

export function ARWaypoint({ label, distance, isDestination = false, style }: ARWaypointProps) {
  return (
    <div
      className="absolute animate-float"
      style={style}
      role="img"
      aria-label={`Waypoint: ${label || "Navigation point"}${distance ? `, ${distance} away` : ""}`}
    >
      <div className={`relative flex flex-col items-center ${isDestination ? "scale-125" : ""}`}>
        {/* Waypoint marker */}
        <div className={`w-8 h-8 rounded-full ${isDestination ? "bg-success" : "bg-primary"} flex items-center justify-center shadow-lg`}
          style={{ boxShadow: `0 0 20px ${isDestination ? "var(--success)" : "var(--primary)"}` }}>
          {isDestination ? (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3">
              <path d="M20 6L9 17l-5-5" />
            </svg>
          ) : (
            <div className="w-3 h-3 bg-white rounded-full" />
          )}
        </div>
        
        {/* Label */}
        {label && (
          <div className="mt-2 px-3 py-1 glass rounded-lg">
            <span className="text-sm font-medium text-foreground whitespace-nowrap">{label}</span>
            {distance && (
              <span className="ml-2 text-xs text-foreground-muted">{distance}</span>
            )}
          </div>
        )}
        
        {/* Vertical line to ground */}
        <div className={`w-0.5 h-16 ${isDestination ? "bg-success/50" : "bg-primary/50"} mt-1`} />
      </div>
    </div>
  );
}

interface DirectionIndicatorProps {
  instruction: string;
  distance: string;
  icon?: "straight" | "left" | "right" | "destination";
}

export function DirectionIndicator({ instruction, distance, icon = "straight" }: DirectionIndicatorProps) {
  const icons = {
    straight: (
      <path d="M12 19V5M5 12l7-7 7 7" />
    ),
    left: (
      <path d="M19 12H5M12 5l-7 7 7 7" />
    ),
    right: (
      <path d="M5 12h14M12 5l7 7-7 7" />
    ),
    destination: (
      <>
        <circle cx="12" cy="10" r="3" />
        <path d="M12 2a8 8 0 0 0-8 8c0 5.4 7 12 8 12s8-6.6 8-12a8 8 0 0 0-8-8z" />
      </>
    ),
  };

  return (
    <div className="glass rounded-2xl px-4 py-3 flex items-center gap-4" role="status" aria-live="polite">
      <div className="w-12 h-12 rounded-xl bg-primary/20 flex items-center justify-center">
        <svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="text-primary"
          aria-hidden="true"
        >
          {icons[icon]}
        </svg>
      </div>
      <div className="flex-1">
        <p className="text-lg font-semibold text-foreground">{instruction}</p>
        <p className="text-sm text-foreground-muted">{distance}</p>
      </div>
    </div>
  );
}

