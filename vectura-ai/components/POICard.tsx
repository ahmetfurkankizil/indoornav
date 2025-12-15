"use client";

import Link from "next/link";

interface POICardProps {
  icon: React.ReactNode;
  label: string;
  href?: string;
  onClick?: () => void;
}

export default function POICard({ icon, label, href, onClick }: POICardProps) {
  const content = (
    <>
      <div className="w-14 h-14 rounded-2xl bg-primary/10 border border-primary/30 flex items-center justify-center text-primary group-hover:bg-primary/20 group-hover:border-primary/50 transition-all">
        {icon}
      </div>
      <span className="text-sm font-medium text-foreground text-center mt-2 line-clamp-2">
        {label}
      </span>
    </>
  );

  const className = "group flex flex-col items-center p-3 rounded-2xl hover:bg-background-card/50 transition-colors min-h-[100px] focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:ring-offset-background outline-none";

  if (href) {
    return (
      <Link href={href} className={className} aria-label={`Navigate to ${label}`}>
        {content}
      </Link>
    );
  }

  return (
    <button onClick={onClick} className={className} aria-label={`Select ${label}`}>
      {content}
    </button>
  );
}

// Common POI icons
export const POIIcons = {
  restroom: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16 4h2a2 2 0 0 1 2 2v1a3 3 0 1 1-6 0V6a2 2 0 0 1 2-2Z" />
      <path d="M4 4h2a2 2 0 0 1 2 2v1a3 3 0 1 1-6 0V6a2 2 0 0 1 2-2Z" />
      <path d="M17 11h2v10h-2z" />
      <path d="M5 11h2v10H5z" />
      <path d="M14 13h5" />
      <path d="M5 13h5" />
    </svg>
  ),
  elevator: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M12 3v18" />
      <path d="m7 9 2-2 2 2" />
      <path d="m13 15 2 2 2-2" />
    </svg>
  ),
  stairs: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 20h4v-4h4v-4h4V8h4V4" />
    </svg>
  ),
  exit: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  ),
  cafeteria: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8h1a4 4 0 0 1 0 8h-1" />
      <path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z" />
      <line x1="6" y1="1" x2="6" y2="4" />
      <line x1="10" y1="1" x2="10" y2="4" />
      <line x1="14" y1="1" x2="14" y2="4" />
    </svg>
  ),
  reception: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="5" />
      <path d="M20 21a8 8 0 1 0-16 0" />
    </svg>
  ),
  parking: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M9 17V7h4a3 3 0 0 1 0 6H9" />
    </svg>
  ),
  medical: (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z" />
      <path d="M10 14h4M12 12v4" />
    </svg>
  ),
};

