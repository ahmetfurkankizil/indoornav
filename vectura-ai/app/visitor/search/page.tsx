"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import PhoneFrame from "@/components/PhoneFrame";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import SearchInput from "@/components/SearchInput";
import POICard, { POIIcons } from "@/components/POICard";

const recentSearches = [
  { id: 1, name: "Room 203", type: "Office" },
  { id: 2, name: "Cafeteria", type: "Food & Drinks" },
  { id: 3, name: "Dr. Smith", type: "Person" },
];

const quickRooms = ["101", "203", "305", "401", "502", "B12"];

export default function VisitorSearchPage() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState("");

  const handleNavigate = () => {
    router.push("/visitor/navigate");
  };

  const filteredRooms = quickRooms.filter(room => 
    room.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <PhoneFrame>
      <div className="relative w-full h-full bg-background overflow-hidden">
        {/* Header */}
        <Header
          title="Memorial Hospital"
          subtitle="Building A - Floor 2"
          showBack
          backHref="/visitor/init"
        />

        {/* Scrollable content */}
        <div className="absolute top-16 bottom-20 left-0 right-0 overflow-y-auto px-4 pt-14">
          <div className="animate-fade-in space-y-6 pb-4">
            {/* Search Input */}
            <div className="pt-2">
              <SearchInput
                placeholder="Search rooms, people, places..."
                value={searchQuery}
                onChange={setSearchQuery}
                onSubmit={handleNavigate}
                autoFocus
              />
            </div>

            {/* Recent Searches */}
            {searchQuery === "" && (
              <section aria-labelledby="recent-heading">
                <h2 id="recent-heading" className="text-sm font-semibold text-foreground-muted uppercase tracking-wide mb-3">
                  Recent Searches
                </h2>
                <div className="space-y-2">
                  {recentSearches.map((item) => (
                    <button
                      key={item.id}
                      onClick={handleNavigate}
                      className="w-full flex items-center gap-4 p-4 bg-background-card rounded-xl hover:bg-background-card/80 transition-colors text-left"
                      aria-label={`Navigate to ${item.name}`}
                    >
                      <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center">
                        <svg
                          width="18"
                          height="18"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          className="text-primary"
                        >
                          <circle cx="12" cy="12" r="10" />
                          <polyline points="12 6 12 12 16 14" />
                        </svg>
                      </div>
                      <div className="flex-1">
                        <p className="font-medium text-foreground">{item.name}</p>
                        <p className="text-sm text-foreground-muted">{item.type}</p>
                      </div>
                      <svg
                        width="20"
                        height="20"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        className="text-foreground-muted"
                      >
                        <path d="M9 18l6-6-6-6" />
                      </svg>
                    </button>
                  ))}
                </div>
              </section>
            )}

            {/* Quick Room Numbers */}
            <section aria-labelledby="rooms-heading">
              <h2 id="rooms-heading" className="text-sm font-semibold text-foreground-muted uppercase tracking-wide mb-3">
                {searchQuery ? "Matching Rooms" : "Quick Select Room"}
              </h2>
              <div className="flex flex-wrap gap-2">
                {(searchQuery ? filteredRooms : quickRooms).map((room) => (
                  <button
                    key={room}
                    onClick={handleNavigate}
                    className="px-5 py-3 bg-background-card border border-primary/30 rounded-xl font-mono text-lg text-primary hover:bg-primary/10 hover:border-primary transition-all min-w-[72px]"
                    aria-label={`Navigate to Room ${room}`}
                  >
                    {room}
                  </button>
                ))}
                {searchQuery && filteredRooms.length === 0 && (
                  <p className="text-foreground-muted py-2">No matching rooms found</p>
                )}
              </div>
            </section>

            {/* Points of Interest */}
            {searchQuery === "" && (
              <section aria-labelledby="poi-heading">
                <h2 id="poi-heading" className="text-sm font-semibold text-foreground-muted uppercase tracking-wide mb-3">
                  Points of Interest
                </h2>
                <div className="grid grid-cols-4 gap-2">
                  <POICard icon={POIIcons.restroom} label="Restroom" onClick={handleNavigate} />
                  <POICard icon={POIIcons.elevator} label="Elevator" onClick={handleNavigate} />
                  <POICard icon={POIIcons.stairs} label="Stairs" onClick={handleNavigate} />
                  <POICard icon={POIIcons.exit} label="Exit" onClick={handleNavigate} />
                  <POICard icon={POIIcons.cafeteria} label="Cafeteria" onClick={handleNavigate} />
                  <POICard icon={POIIcons.reception} label="Reception" onClick={handleNavigate} />
                  <POICard icon={POIIcons.parking} label="Parking" onClick={handleNavigate} />
                  <POICard icon={POIIcons.medical} label="Emergency" onClick={handleNavigate} />
                </div>
              </section>
            )}

            {/* Floor selector */}
            {searchQuery === "" && (
              <section aria-labelledby="floor-heading">
                <h2 id="floor-heading" className="text-sm font-semibold text-foreground-muted uppercase tracking-wide mb-3">
                  Change Floor
                </h2>
                <div className="flex gap-2 overflow-x-auto pb-2">
                  {["B1", "G", "1", "2", "3", "4", "5"].map((floor, i) => (
                    <button
                      key={floor}
                      className={`flex-shrink-0 w-12 h-12 rounded-xl font-medium text-lg transition-all ${
                        floor === "2"
                          ? "bg-primary text-background"
                          : "bg-background-card text-foreground hover:bg-background-card/80"
                      }`}
                      aria-label={`Floor ${floor}`}
                      aria-current={floor === "2" ? "true" : undefined}
                    >
                      {floor}
                    </button>
                  ))}
                </div>
              </section>
            )}
          </div>
        </div>

        {/* Footer */}
        <Footer>
          <button
            onClick={handleNavigate}
            className="w-full h-14 bg-primary text-background font-semibold text-lg rounded-2xl hover:bg-primary-dark transition-colors flex items-center justify-center gap-2"
            aria-label="Start navigation"
          >
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <polygon points="3 11 22 2 13 21 11 13 3 11" />
            </svg>
            Start Navigation
          </button>
        </Footer>
      </div>
    </PhoneFrame>
  );
}

