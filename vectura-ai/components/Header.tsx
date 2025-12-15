"use client";

import Link from "next/link";

interface HeaderProps {
  title: string;
  subtitle?: string;
  showBack?: boolean;
  backHref?: string;
  rightElement?: React.ReactNode;
  variant?: "default" | "transparent" | "admin";
}

export default function Header({
  title,
  subtitle,
  showBack = false,
  backHref = "/",
  rightElement,
  variant = "default",
}: HeaderProps) {
  const bgClasses = {
    default: "bg-background-secondary/95 backdrop-blur-md border-b border-primary/20",
    transparent: "bg-transparent",
    admin: "bg-secondary/20 backdrop-blur-md border-b border-secondary/30",
  };

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-40 h-16 ${bgClasses[variant]}`}
      role="banner"
    >
      {/* Safe area for notch */}
      <div className="h-[50px]" />
      
      <div className="flex items-center justify-between px-4 h-[calc(100%-50px)] pb-2">
        {/* Left section */}
        <div className="flex items-center gap-3 min-w-[48px]">
          {showBack && (
            <Link
              href={backHref}
              className="flex items-center justify-center w-12 h-12 rounded-full bg-background-card/50 text-primary hover:bg-background-card transition-colors"
              aria-label="Go back"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
            </Link>
          )}
        </div>

        {/* Center section */}
        <div className="flex-1 text-center">
          <h1 className="text-lg font-semibold text-foreground truncate">
            {title}
          </h1>
          {subtitle && (
            <p className="text-xs text-foreground-muted truncate">{subtitle}</p>
          )}
        </div>

        {/* Right section */}
        <div className="flex items-center gap-2 min-w-[48px] justify-end">
          {rightElement}
        </div>
      </div>
    </header>
  );
}

