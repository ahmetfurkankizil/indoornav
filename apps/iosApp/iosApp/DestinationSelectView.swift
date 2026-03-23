import SwiftUI

/// Destination selection screen with search field and room list.
/// Rooms are loaded from the building config — no hardcoded fallbacks.
struct DestinationSelectView: View {
    @ObservedObject var flow: NavigationFlowModel
    @State private var searchText = ""

    private var filteredRooms: [BuildingPackageLoader.PackageRoom] {
        let rooms = flow.availableRooms
        if searchText.isEmpty { return rooms }
        return rooms.filter {
            $0.displayName.localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: { flow.endNavigation() }) {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundStyle(.blue)
                }

                Spacer()

                Text("Select Destination")
                    .font(.headline)

                Spacer()

                // Balance spacer
                Image(systemName: "chevron.left")
                    .font(.title3)
                    .hidden()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            // Entrance badge
            HStack(spacing: 8) {
                Image(systemName: "location.circle.fill")
                    .foregroundStyle(.green)
                Text("Starting from \(flow.confirmedEntrance)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            // Search field
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField("Search rooms...", text: $searchText)
                    .textFieldStyle(.plain)
                if !searchText.isEmpty {
                    Button(action: { searchText = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)

            Divider()

            // Room list
            if filteredRooms.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "magnifyingglass")
                        .font(.largeTitle)
                        .foregroundStyle(.secondary)
                    Text("No rooms found")
                        .foregroundStyle(.secondary)
                    Spacer()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(filteredRooms, id: \.id) { room in
                            Button(action: { flow.selectDestination(room) }) {
                                HStack {
                                    Image(systemName: iconForCategory(room.category))
                                        .foregroundStyle(.blue)
                                        .frame(width: 32)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(room.displayName)
                                            .font(.body)
                                            .foregroundStyle(.primary)
                                        if let category = room.category, category != "unknown" {
                                            Text(category.capitalized)
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                        }
                                    }

                                    Spacer()

                                    Image(systemName: "chevron.right")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                            }
                            Divider().padding(.leading, 56)
                        }
                    }
                }
            }
        }
    }

    private func iconForCategory(_ category: String?) -> String {
        switch category {
        case "kitchen": return "fork.knife"
        case "living_room", "salon": return "sofa"
        case "bedroom": return "bed.double"
        case "bathroom": return "shower"
        case "office": return "desktopcomputer"
        default: return "door.left.hand.open"
        }
    }
}
