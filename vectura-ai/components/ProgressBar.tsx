"use client";

interface ProgressBarProps {
  value: number;
  max?: number;
  label?: string;
  showPercentage?: boolean;
  variant?: "primary" | "secondary" | "success" | "warning";
  size?: "sm" | "md" | "lg";
  animated?: boolean;
}

export default function ProgressBar({
  value,
  max = 100,
  label,
  showPercentage = true,
  variant = "primary",
  size = "md",
  animated = true,
}: ProgressBarProps) {
  const percentage = Math.min(Math.max((value / max) * 100, 0), 100);

  const variantClasses = {
    primary: "bg-primary",
    secondary: "bg-secondary",
    success: "bg-success",
    warning: "bg-warning",
  };

  const glowClasses = {
    primary: "shadow-[0_0_10px_var(--primary-glow)]",
    secondary: "shadow-[0_0_10px_var(--secondary-glow)]",
    success: "shadow-[0_0_10px_rgba(16,185,129,0.3)]",
    warning: "shadow-[0_0_10px_rgba(245,158,11,0.3)]",
  };

  const sizeClasses = {
    sm: "h-1",
    md: "h-2",
    lg: "h-3",
  };

  return (
    <div className="w-full" role="progressbar" aria-valuenow={value} aria-valuemin={0} aria-valuemax={max} aria-label={label}>
      {(label || showPercentage) && (
        <div className="flex justify-between items-center mb-2">
          {label && (
            <span className="text-sm font-medium text-foreground">{label}</span>
          )}
          {showPercentage && (
            <span className="text-sm font-mono text-primary">
              {Math.round(percentage)}%
            </span>
          )}
        </div>
      )}
      
      <div className={`w-full bg-background-card rounded-full overflow-hidden ${sizeClasses[size]}`}>
        <div
          className={`h-full rounded-full transition-all duration-500 ease-out relative ${variantClasses[variant]} ${glowClasses[variant]}`}
          style={{ width: `${percentage}%` }}
        >
          {/* Shine effect */}
          {animated && percentage > 0 && percentage < 100 && (
            <div className="absolute inset-0 overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-progress-shine" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

