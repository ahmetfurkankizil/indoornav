import SwiftUI

/// Destination selection screen — standard iOS list with search.
/// Rooms grouped by category when browsing; flat filtered list when searching.
struct DestinationSelectView: View {
    @ObservedObject var flow: NavigationFlowModel
    @State private var searchText = ""
    @FocusState private var searchFocused: Bool

    private var filteredRooms: [BuildingPackageLoader.PackageRoom] {
        let rooms = flow.availableRooms
        guard !searchText.isEmpty else { return rooms }
        return rooms.filter {
            $0.displayName.localizedCaseInsensitiveContains(searchText) ||
            ($0.category?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    private var groupedRooms: [(String, [BuildingPackageLoader.PackageRoom])] {
        let grouped = Dictionary(grouping: filteredRooms) { $0.category ?? "other" }
        let order = ["kitchen", "living_room", "bedroom", "bathroom", "office", "other"]
        return order.compactMap { cat in
            guard let r = grouped[cat], !r.isEmpty else { return nil }
            return (cat, r)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Navigation bar
            HStack {
                Button(action: { flow.endNavigation() }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .fontWeight(.semibold)
                        Text("Back")
                    }
                    .font(.body)
                    .foregroundStyle(.blue)
                }

                Spacer()

                Text("Choose Destination")
                    .font(.headline)

                Spacer()

                // Balance
                Text("Back")
                    .font(.body)
                    .hidden()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            // Origin badge
            HStack(spacing: 6) {
                Image(systemName: "location.circle.fill")
                    .foregroundStyle(.green)
                    .font(.footnote)
                Text("From: \(flow.confirmedEntrance)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 10)

            // Search bar
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color(.tertiaryLabel))
                    .font(.subheadline)
                TextField("Search rooms…", text: $searchText)
                    .font(.body)
                    .focused($searchFocused)
                    .submitLabel(.search)
                if !searchText.isEmpty {
                    Button(action: { searchText = ""; searchFocused = false }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Color(.tertiaryLabel))
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 9)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            Divider()

            // Room list
            if filteredRooms.isEmpty {
                emptyState
            } else {
                roomList
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }

    // MARK: - Room List

    @ViewBuilder
    private var roomList: some View {
        if !searchText.isEmpty {
            // Flat list when searching
            List(filteredRooms, id: \.id) { room in
                roomRow(room)
                    .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
            }
            .listStyle(.plain)
        } else {
            // Grouped by category
            List {
                ForEach(groupedRooms, id: \.0) { category, rooms in
                    Section(header: sectionHeader(category)) {
                        ForEach(rooms, id: \.id) { room in
                            roomRow(room)
                                .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }

    private func sectionHeader(_ category: String) -> some View {
        Text(displayNameForCategory(category))
            .font(.footnote).fontWeight(.semibold)
            .foregroundStyle(.secondary)
            .textCase(.uppercase)
    }

    private func roomRow(_ room: BuildingPackageLoader.PackageRoom) -> some View {
        Button(action: { flow.selectDestination(room) }) {
            HStack(spacing: 12) {
                // Icon
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(categoryColor(room.category).opacity(0.12))
                        .frame(width: 36, height: 36)
                    Image(systemName: iconForCategory(room.category))
                        .font(.system(size: 16))
                        .foregroundStyle(categoryColor(room.category))
                }

                // Text
                VStack(alignment: .leading, spacing: 2) {
                    Text(room.displayName)
                        .font(.body)
                        .foregroundStyle(.primary)
                    if let desc = room.description, !desc.isEmpty {
                        Text(desc)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    } else {
                        Text(displayNameForCategory(room.category ?? "other"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Spacer(minLength: 8)

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color(.tertiaryLabel))
            }
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "magnifyingglass")
                .font(.system(size: 36))
                .foregroundStyle(Color(.tertiaryLabel))
            Text("No rooms match \"\(searchText)\"")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, 32)
    }

    // MARK: - Helpers

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

    private func categoryColor(_ category: String?) -> Color {
        switch category {
        case "kitchen": return .orange
        case "living_room", "salon": return .blue
        case "bedroom": return .indigo
        case "bathroom": return .teal
        case "office": return .green
        default: return Color(.systemGray)
        }
    }

    private func displayNameForCategory(_ category: String) -> String {
        switch category {
        case "kitchen": return "Kitchen"
        case "living_room", "salon": return "Living Room"
        case "bedroom": return "Bedroom"
        case "bathroom": return "Bathroom"
        case "office": return "Office"
        default: return "Other"
        }
    }
}
