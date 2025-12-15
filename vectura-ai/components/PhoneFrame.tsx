"use client";

import { ReactNode } from "react";

interface PhoneFrameProps {
  children: ReactNode;
}

export default function PhoneFrame({ children }: PhoneFrameProps) {
  return (
    <div className="min-h-screen bg-[#1a1a2e] flex items-center justify-center p-4">
      {/* Phone frame container */}
      <div className="relative">
        {/* Phone outer bezel */}
        <div className="relative w-[395px] h-[812px] bg-[#1c1c1e] rounded-[50px] p-[10px] shadow-2xl shadow-black/50">
          {/* Phone inner bezel */}
          <div className="relative w-full h-full bg-background rounded-[40px] overflow-hidden">
            {/* Dynamic Island / Notch */}
            <div className="absolute top-0 left-0 right-0 z-50 flex justify-center pt-3">
              <div className="w-[126px] h-[37px] bg-black rounded-full flex items-center justify-center gap-2">
                <div className="w-3 h-3 rounded-full bg-[#1c1c1e] ring-1 ring-gray-700" />
                <div className="w-2 h-2 rounded-full bg-[#0a84ff]" />
              </div>
            </div>
            
            {/* Screen content area - 375px internal width */}
            <div className="w-[375px] h-full mx-auto overflow-hidden relative">
              {children}
            </div>
            
            {/* Home indicator */}
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-[134px] h-[5px] bg-white/30 rounded-full" />
          </div>
        </div>
        
        {/* Side buttons */}
        <div className="absolute left-[-2px] top-[120px] w-[3px] h-[30px] bg-[#2c2c2e] rounded-l-sm" />
        <div className="absolute left-[-2px] top-[170px] w-[3px] h-[60px] bg-[#2c2c2e] rounded-l-sm" />
        <div className="absolute left-[-2px] top-[240px] w-[3px] h-[60px] bg-[#2c2c2e] rounded-l-sm" />
        <div className="absolute right-[-2px] top-[180px] w-[3px] h-[80px] bg-[#2c2c2e] rounded-r-sm" />
      </div>
    </div>
  );
}

