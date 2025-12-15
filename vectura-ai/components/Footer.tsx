"use client";

interface FooterProps {
  children: React.ReactNode;
  variant?: "default" | "navigation" | "transparent";
}

export default function Footer({ children, variant = "default" }: FooterProps) {
  const bgClasses = {
    default: "bg-background-secondary/95 backdrop-blur-md border-t border-primary/20",
    navigation: "bg-gradient-to-t from-background via-background/95 to-transparent border-t border-primary/10",
    transparent: "bg-transparent",
  };

  return (
    <footer
      className={`fixed bottom-0 left-0 right-0 z-40 ${bgClasses[variant]}`}
      role="contentinfo"
    >
      <div className="px-4 py-3 pb-8">
        {children}
      </div>
    </footer>
  );
}

