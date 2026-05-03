import SwiftUI

/// Destination selection screen.
/// Rooms are grouped by category while browsing and shown as a flat list while searching.
/// Also hosts the conversational AI route assistant (`AiAssistantView`).
struct DestinationSelectView: View {
    @ObservedObject var flow: NavigationFlowModel
    @StateObject private var assistantViewModel: AiAssistantViewModel
    @State private var searchText = ""
    @State private var selectedFilter: DestinationFilter = .all
    @FocusState private var searchFocused: Bool

    init(flow: NavigationFlowModel) {
        self.flow = flow
        _assistantViewModel = StateObject(wrappedValue: AiAssistantViewModel(flow: flow))
    }

    private var orderedRooms: [BuildingPackageLoader.PackageRoom] {
        flow.availableRooms.sorted { destinationSortIndex($0.id) < destinationSortIndex($1.id) }
    }

    private var filteredRooms: [BuildingPackageLoader.PackageRoom] {
        orderedRooms.filter { room in
            let matchesSearch = searchText.isEmpty ||
                room.displayName.localizedCaseInsensitiveContains(searchText) ||
                (room.description?.localizedCaseInsensitiveContains(searchText) ?? false) ||
                (room.category?.localizedCaseInsensitiveContains(searchText) ?? false)
            return matchesSearch && selectedFilter.matches(room)
        }
    }

    private var groupedRooms: [(String, [BuildingPackageLoader.PackageRoom])] {
        let grouped = Dictionary(grouping: filteredRooms) { $0.category ?? "other" }
        let preferred = ["classroom", "lab", "cafe", "vertical_transport", "toilet",
                         "kitchen", "living_room", "bedroom", "bathroom", "office", "other"]
        let ordered = preferred.filter { grouped[$0] != nil } +
            grouped.keys.sorted().filter { !preferred.contains($0) }

        return ordered.compactMap { category in
            guard let rooms = grouped[category], !rooms.isEmpty else { return nil }
            return (category, rooms)
        }
    }

    var body: some View {
        ZStack {
            VecturBackground()

            VStack(spacing: 0) {
                DestinationTopBar(onBack: { flow.endNavigation() })
                    .padding(.horizontal, 20)
                    .padding(.top, 8)

                FlowProgressStrip(activeStep: 1)
                    .padding(.horizontal, 24)
                    .padding(.top, 10)

                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .bottom) {
                        VStack(alignment: .leading, spacing: 5) {
                            Text("Choose destination")
                                .font(.system(size: 30, weight: .bold, design: .rounded))
                                .foregroundStyle(VecturTheme.textPrimary)
                            Text("From \(flow.confirmedEntrance.isEmpty ? "Main Entrance" : flow.confirmedEntrance)")
                                .font(.subheadline.weight(.medium))
                                .foregroundStyle(VecturTheme.textMuted)
                        }
                        Spacer()
                        VecturStatPill(text: "\(filteredRooms.count) Places", color: VecturTheme.cyan)
                    }

                    SearchField(searchText: $searchText, focused: $searchFocused)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(DestinationFilter.allCases, id: \.self) { filter in
                                FilterChip(
                                    text: filter.title,
                                    selected: selectedFilter == filter,
                                    action: { selectedFilter = filter }
                                )
                            }
                        }
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 22)

                if filteredRooms.isEmpty {
                    emptyState
                } else {
                    roomScroll
                }
            }
            .safeAreaPadding(.top)
            .safeAreaPadding(.bottom)

            if let result = assistantViewModel.assistantResult {
                AssistantConfirmationOverlay(
                    result: result,
                    onConfirm: { assistantViewModel.acceptAndStartNavigation() },
                    onPickAlternative: { candidate in
                        assistantViewModel.chooseAlternativeCandidate(candidate)
                    },
                    onCancel: { assistantViewModel.cancel() }
                )
                .transition(.opacity)
                .zIndex(10)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(VecturTheme.canvas)
    }

    private var roomScroll: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 18) {
                if assistantViewModel.isAssistantAvailable {
                    AiAssistantView(viewModel: assistantViewModel)
                }

                if searchText.isEmpty && selectedFilter == .all && !recentRooms.isEmpty {
                    VecturSectionHeader(title: "Recently Visited")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(recentRooms, id: \.id) { room in
                                RecentDestinationCard(
                                    room: room,
                                    routeSummary: routeSummary(for: room),
                                    action: { flow.selectDestination(room) }
                                )
                            }
                        }
                    }
                }

                VecturSectionHeader(
                    title: searchText.isEmpty ? "Locations" : "Search Results",
                    trailing: "\(filteredRooms.count)"
                )

                if searchText.isEmpty {
                    ForEach(groupedRooms, id: \.0) { category, rooms in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(VecturTheme.categoryName(category))
                                .font(.caption.weight(.bold))
                                .foregroundStyle(VecturTheme.textMuted)
                                .textCase(.uppercase)
                            ForEach(rooms, id: \.id) { room in
                                DestinationRoomCard(
                                    room: room,
                                    routeSummary: routeSummary(for: room),
                                    action: { flow.selectDestination(room) }
                                )
                            }
                        }
                    }
                } else {
                    ForEach(filteredRooms, id: \.id) { room in
                        DestinationRoomCard(
                            room: room,
                            routeSummary: routeSummary(for: room),
                            action: { flow.selectDestination(room) }
                        )
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 34)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 14) {
            Spacer()
            Image(systemName: "magnifyingglass")
                .font(.system(size: 38, weight: .semibold))
                .foregroundStyle(VecturTheme.textDisabled)
            Text("No places found")
                .font(.headline)
                .foregroundStyle(VecturTheme.textPrimary)
            Text(searchText.isEmpty ? "Try another filter." : "No rooms match \"\(searchText)\".")
                .font(.subheadline)
                .foregroundStyle(VecturTheme.textMuted)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, 32)
    }

    private var recentRooms: [BuildingPackageLoader.PackageRoom] {
        let preferred = ["cs-lab", "fameo-cafe"]
        let rooms = preferred.compactMap { id in orderedRooms.first { $0.id == id } }
        return rooms.isEmpty ? Array(orderedRooms.prefix(2)) : rooms
    }

    private func routeSummary(for room: BuildingPackageLoader.PackageRoom) -> RouteSummary? {
        guard let config = flow.reviewedConfig,
              let package = BuildingPackageLoader.computeRoute(config: config, destinationRoomId: room.id) else {
            return nil
        }
        return RouteSummary(
            distance: package.totalDistance,
            stepCount: max(1, package.arrows.filter { $0.type != .follow }.count)
        )
    }

    private func destinationSortIndex(_ id: String) -> Int {
        switch id {
        case "ea101": return 0
        case "ea102": return 1
        case "cs-lab": return 2
        case "me-lab": return 3
        case "fameo-cafe": return 4
        case "elevators": return 5
        case "west-men-wc": return 6
        case "west-women-wc": return 7
        case "east-men-wc": return 8
        case "east-women-wc": return 9
        default: return 100
        }
    }
}

private struct DestinationTopBar: View {
    let onBack: () -> Void

    var body: some View {
        HStack {
            VecturIconChip(systemName: "chevron.left", action: onBack, tint: VecturTheme.textSecondary)
            Spacer()
            Text("Destination")
                .font(.headline)
                .foregroundStyle(VecturTheme.textPrimary)
            Spacer()
            Color.clear.frame(width: 48, height: 48)
        }
    }
}

private struct SearchField: View {
    @Binding var searchText: String
    var focused: FocusState<Bool>.Binding

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(VecturTheme.textMuted)
            TextField("Search rooms", text: $searchText)
                .font(.body.weight(.medium))
                .foregroundStyle(VecturTheme.textPrimary)
                .focused(focused)
                .submitLabel(.search)
            if !searchText.isEmpty {
                Button(action: { searchText = ""; focused.wrappedValue = false }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(VecturTheme.textMuted)
                }
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 54)
        .background(VecturTheme.elevated.opacity(0.9))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(VecturTheme.borderSubtle, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct FilterChip: View {
    let text: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(selected ? .white : VecturTheme.textSecondary)
                .padding(.horizontal, 16)
                .frame(height: 44)
                .background(selected ? AnyShapeStyle(VecturTheme.primaryGradient) : AnyShapeStyle(VecturTheme.elevated))
                .overlay(
                    Capsule().stroke(selected ? .clear : VecturTheme.borderSubtle, lineWidth: 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct DestinationRoomCard: View {
    let room: BuildingPackageLoader.PackageRoom
    let routeSummary: RouteSummary?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VecturCard {
                HStack(spacing: 14) {
                    CategoryIcon(category: room.category)

                    VStack(alignment: .leading, spacing: 5) {
                        HStack(spacing: 8) {
                            Text(room.displayName)
                                .font(.headline)
                                .foregroundStyle(VecturTheme.textPrimary)
                                .lineLimit(1)
                            Spacer(minLength: 8)
                            VecturStatPill(
                                text: VecturTheme.categoryName(room.category),
                                color: VecturTheme.categoryColor(room.category)
                            )
                        }

                        Text(room.description?.isEmpty == false ? room.description! : locationSubtitle(for: room))
                            .font(.subheadline)
                            .foregroundStyle(VecturTheme.textMuted)
                            .lineLimit(2)

                        HStack(spacing: 10) {
                            if let routeSummary {
                                Label(formatMeters(routeSummary.distance), systemImage: "figure.walk")
                                Text("\(routeSummary.stepCount) \(routeSummary.stepCount == 1 ? "step" : "steps")")
                            }
                            if let floor = room.floorName, !floor.isEmpty {
                                Text(floor)
                            }
                        }
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(VecturTheme.textDisabled)
                    }

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(VecturTheme.textDisabled)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

private struct RecentDestinationCard: View {
    let room: BuildingPackageLoader.PackageRoom
    let routeSummary: RouteSummary?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 12) {
                CategoryIcon(category: room.category)
                VStack(alignment: .leading, spacing: 4) {
                    Text(room.displayName)
                        .font(.headline)
                        .foregroundStyle(VecturTheme.textPrimary)
                        .lineLimit(1)
                    Text(routeSummary.map { formatMeters($0.distance) } ?? locationSubtitle(for: room))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(VecturTheme.textMuted)
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .frame(width: 166, height: 138, alignment: .leading)
            .background(VecturTheme.card.opacity(0.96))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(VecturTheme.borderSubtle, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct CategoryIcon: View {
    let category: String?

    var body: some View {
        let color = VecturTheme.categoryColor(category)
        Image(systemName: VecturTheme.categoryIcon(category))
            .font(.system(size: 20, weight: .semibold))
            .foregroundStyle(color)
            .frame(width: 46, height: 46)
            .background(color.opacity(0.16))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(color.opacity(0.32), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct RouteSummary {
    let distance: Double
    let stepCount: Int
}

private enum DestinationFilter: CaseIterable {
    case all
    case classes
    case labs
    case amenities

    var title: String {
        switch self {
        case .all: return "All"
        case .classes: return "Classes"
        case .labs: return "Labs"
        case .amenities: return "Amenities"
        }
    }

    func matches(_ room: BuildingPackageLoader.PackageRoom) -> Bool {
        switch self {
        case .all:
            return true
        case .classes:
            return room.category == "classroom"
        case .labs:
            return room.category == "lab"
        case .amenities:
            return ["cafe", "vertical_transport", "toilet", "kitchen", "bathroom"].contains(room.category ?? "")
        }
    }
}

private struct FlowProgressStrip: View {
    let activeStep: Int
    private let steps = ["Entrance", "Destination", "Navigate"]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(steps.indices, id: \.self) { index in
                let active = index <= activeStep
                Text(index < activeStep ? "\(steps[index]) done" : steps[index])
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(active ? VecturTheme.textPrimary : VecturTheme.textMuted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(active ? VecturTheme.blue.opacity(0.22) : VecturTheme.elevated.opacity(0.72))
                    .overlay(Capsule().stroke(active ? VecturTheme.blue.opacity(0.5) : VecturTheme.borderSubtle, lineWidth: 1))
                    .clipShape(Capsule())

                if index != steps.count - 1 {
                    Rectangle()
                        .fill(index < activeStep ? VecturTheme.blue : VecturTheme.borderSubtle)
                        .frame(height: 1)
                }
            }
        }
    }
}

private func locationSubtitle(for room: BuildingPackageLoader.PackageRoom) -> String {
    if let floor = room.floorName, !floor.isEmpty {
        return floor
    }
    return VecturTheme.categoryName(room.category)
}

private func formatMeters(_ meters: Double) -> String {
    "\(String(format: "%.0f", meters)) m"
}
