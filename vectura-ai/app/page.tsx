"use client";

import Link from "next/link";
import PhoneFrame from "@/components/PhoneFrame";

const screens = [
  {
    href: "/visitor/init",
    title: "Scan QR Code",
    description: "Initialize AR navigation by scanning a building QR code",
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="4" height="4" rx="0.5" />
        <path d="M21 14v3a2 2 0 01-2 2h-3" />
      </svg>
    ),
    color: "primary",
    role: "visitor",
  },
  {
    href: "/visitor/search",
    title: "Find Destination",
    description: "Search for rooms, people, or points of interest",
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
      </svg>
    ),
    color: "primary",
    role: "visitor",
  },
  {
    href: "/visitor/navigate",
    title: "AR Navigation",
    description: "Follow AR arrows to your destination in real-time",
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <polygon points="3 11 22 2 13 21 11 13 3 11" />
      </svg>
    ),
    color: "primary",
    role: "visitor",
  },
  {
    href: "/admin/dashboard",
    title: "Admin Dashboard",
    description: "LiDAR scanning, cloud upload, and map processing",
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
      </svg>
    ),
    color: "secondary",
    role: "admin",
  },
];

export default function HomePage() {
  return (
    <PhoneFrame>
      <div className="relative w-full h-full bg-background overflow-hidden">
        {/* Background decoration */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute top-20 -left-20 w-64 h-64 bg-primary/5 rounded-full blur-3xl" />
          <div className="absolute bottom-40 -right-20 w-64 h-64 bg-secondary/5 rounded-full blur-3xl" />
        </div>

        {/* Content */}
        <div className="relative z-10 h-full overflow-y-auto px-5 pt-20 pb-10">
          <div className="animate-fade-in">
            {/* Logo and title */}
            <div className="text-center mb-8">
              <div className="inline-flex items-center justify-center w-20 h-20 rounded-3xl bg-gradient-to-br from-primary/20 to-secondary/20 border border-primary/30 mb-4">
                <svg
                  width="40"
                  height="40"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  className="text-primary"
                >
                  <polygon points="3 11 22 2 13 21 11 13 3 11" />
                </svg>
              </div>
              <h1 className="text-3xl font-bold text-foreground mb-2">
                Vectura AI
              </h1>
              <p className="text-foreground-muted">
                Indoor AR Navigation System
              </p>
            </div>

            {/* Screen links */}
            <div className="space-y-4">
              {/* Visitor section */}
              <div>
                <h2 className="text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-3 px-1">
                  Visitor Screens
                </h2>
                <div className="space-y-3">
                  {screens
                    .filter((s) => s.role === "visitor")
                    .map((screen) => (
                      <Link
                        key={screen.href}
                        href={screen.href}
                        className="group flex items-center gap-4 p-4 bg-background-card/50 hover:bg-background-card border border-primary/10 hover:border-primary/30 rounded-2xl transition-all"
                      >
                        <div className="w-14 h-14 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center text-primary group-hover:bg-primary/20 group-hover:scale-105 transition-all">
                          {screen.icon}
                        </div>
                        <div className="flex-1 min-w-0">
                          <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors">
                            {screen.title}
                          </h3>
                          <p className="text-sm text-foreground-muted line-clamp-2">
                            {screen.description}
                          </p>
                        </div>
                        <svg
                          width="20"
                          height="20"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          className="text-foreground-muted group-hover:text-primary group-hover:translate-x-1 transition-all flex-shrink-0"
                        >
                          <path d="M9 18l6-6-6-6" />
                        </svg>
                      </Link>
                    ))}
                </div>
              </div>

              {/* Admin section */}
              <div className="pt-2">
                <h2 className="text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-3 px-1">
                  Admin Screens
                </h2>
                <div className="space-y-3">
                  {screens
                    .filter((s) => s.role === "admin")
                    .map((screen) => (
                      <Link
                        key={screen.href}
                        href={screen.href}
                        className="group flex items-center gap-4 p-4 bg-secondary/5 hover:bg-secondary/10 border border-secondary/20 hover:border-secondary/40 rounded-2xl transition-all"
                      >
                        <div className="w-14 h-14 rounded-xl bg-secondary/10 border border-secondary/30 flex items-center justify-center text-secondary group-hover:bg-secondary/20 group-hover:scale-105 transition-all">
                          {screen.icon}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <h3 className="font-semibold text-foreground group-hover:text-secondary transition-colors">
                              {screen.title}
                            </h3>
                            <span className="px-2 py-0.5 text-[10px] font-bold uppercase bg-secondary/20 text-secondary rounded-full">
                              Admin
                            </span>
                          </div>
                          <p className="text-sm text-foreground-muted line-clamp-2">
                            {screen.description}
                          </p>
                        </div>
                        <svg
                          width="20"
                          height="20"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          className="text-foreground-muted group-hover:text-secondary group-hover:translate-x-1 transition-all flex-shrink-0"
                        >
                          <path d="M9 18l6-6-6-6" />
                        </svg>
                      </Link>
                    ))}
                </div>
              </div>
            </div>

            {/* Footer info */}
            <div className="mt-8 text-center">
              <p className="text-xs text-foreground-muted/60">
                High-Fidelity Mobile Prototype
              </p>
              <p className="text-xs text-foreground-muted/40 mt-1">
                Built with Next.js + Tailwind CSS
              </p>
            </div>
          </div>
        </div>
      </div>
    </PhoneFrame>
  );
}
