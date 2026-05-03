import SwiftUI

/// Lightweight SwiftUI style namespace for the visitor-facing iOS flow.
enum VecturTheme {

    // MARK: - Colors

    static let canvas = Color(red: 0.027, green: 0.051, blue: 0.094)
    static let elevated = Color(red: 0.071, green: 0.102, blue: 0.157)
    static let card = Color(red: 0.082, green: 0.122, blue: 0.192)
    static let overlay = Color(red: 0.106, green: 0.141, blue: 0.212)

    static let borderSubtle = Color(red: 0.137, green: 0.192, blue: 0.286)
    static let borderStrong = Color(red: 0.169, green: 0.224, blue: 0.322)

    static let textPrimary = Color(red: 0.945, green: 0.961, blue: 0.976)
    static let textSecondary = Color(red: 0.714, green: 0.749, blue: 0.808)
    static let textMuted = Color(red: 0.557, green: 0.600, blue: 0.682)
    static let textDisabled = Color(red: 0.337, green: 0.380, blue: 0.451)

    static let cyan = Color(red: 0.133, green: 0.827, blue: 0.933)
    static let green = Color(red: 0.071, green: 0.784, blue: 0.416)
    static let amber = Color(red: 0.961, green: 0.620, blue: 0.043)
    static let red = Color(red: 0.937, green: 0.267, blue: 0.267)
    static let blue = Color(red: 0.145, green: 0.388, blue: 0.922)

    static let primaryGradient = LinearGradient(
        colors: [
            Color(red: 0.114, green: 0.306, blue: 0.847),
            Color(red: 0.145, green: 0.388, blue: 0.922),
            Color(red: 0.024, green: 0.714, blue: 0.831)
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let cardBackground = card

    static func categoryColor(_ category: String?) -> Color {
        switch category {
        case "classroom": return .blue
        case "lab": return .purple
        case "cafe", "kitchen": return amber
        case "vertical_transport": return .teal
        case "toilet": return Color(.systemGray2)
        case "living_room", "salon": return cyan
        case "bedroom": return .indigo
        case "bathroom": return .teal
        case "office": return green
        default: return textMuted
        }
    }

    static func categoryName(_ category: String?) -> String {
        switch category {
        case "classroom": return "Classrooms"
        case "lab": return "Laboratories"
        case "cafe": return "Cafe"
        case "vertical_transport": return "Elevators"
        case "toilet": return "Restrooms"
        case "kitchen": return "Kitchen"
        case "living_room", "salon": return "Living Room"
        case "bedroom": return "Bedroom"
        case "bathroom": return "Bathroom"
        case "office": return "Office"
        default: return "Other"
        }
    }

    static func categoryIcon(_ category: String?) -> String {
        switch category {
        case "classroom": return "graduationcap.fill"
        case "lab": return "flask.fill"
        case "cafe": return "cup.and.saucer.fill"
        case "vertical_transport": return "arrow.up.arrow.down"
        case "toilet": return "figure.dress.line.vertical.figure"
        case "kitchen": return "fork.knife"
        case "living_room", "salon": return "sofa.fill"
        case "bedroom": return "bed.double.fill"
        case "bathroom": return "shower.fill"
        case "office": return "desktopcomputer"
        default: return "door.left.hand.open"
        }
    }

    // MARK: - View Modifiers

    struct PrimaryButton: ViewModifier {
        func body(content: Content) -> some View {
            content
                .font(.headline.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 56)
                .foregroundStyle(.white)
                .background(primaryGradient)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .shadow(color: cyan.opacity(0.24), radius: 18, y: 8)
        }
    }

    struct SecondaryButton: ViewModifier {
        func body(content: Content) -> some View {
            content
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 52)
                .foregroundStyle(textSecondary)
                .background(elevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(borderStrong, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }
}

extension View {
    func vecturPrimaryButton() -> some View {
        modifier(VecturTheme.PrimaryButton())
    }

    func vecturSecondaryButton() -> some View {
        modifier(VecturTheme.SecondaryButton())
    }
}

struct VecturBackground: View {
    var showGrid = true

    var body: some View {
        ZStack {
            VecturTheme.canvas

            GeometryReader { proxy in
                Circle()
                    .fill(VecturTheme.cyan.opacity(0.16))
                    .blur(radius: 90)
                    .frame(width: proxy.size.width * 0.9, height: proxy.size.width * 0.9)
                    .offset(x: -proxy.size.width * 0.38, y: -proxy.size.height * 0.2)

                Circle()
                    .fill(VecturTheme.blue.opacity(0.18))
                    .blur(radius: 100)
                    .frame(width: proxy.size.width, height: proxy.size.width)
                    .offset(x: proxy.size.width * 0.38, y: proxy.size.height * 0.42)

                if showGrid {
                    DotGrid()
                        .opacity(0.28)
                }

                LinearGradient(
                    colors: [
                        VecturTheme.canvas.opacity(0.76),
                        .clear,
                        VecturTheme.canvas.opacity(0.9)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .ignoresSafeArea()
    }
}

private struct DotGrid: View {
    var body: some View {
        Canvas { context, size in
            let spacing: CGFloat = 22
            let dot = Path(ellipseIn: CGRect(x: 0, y: 0, width: 1.6, height: 1.6))
            var x: CGFloat = 10
            while x < size.width {
                var y: CGFloat = 14
                while y < size.height {
                    context.fill(dot.offsetBy(dx: x, dy: y), with: .color(VecturTheme.borderStrong))
                    y += spacing
                }
                x += spacing
            }
        }
    }
}

struct VecturCard<Content: View>: View {
    var glass = false
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background((glass ? VecturTheme.card.opacity(0.58) : VecturTheme.card.opacity(0.96)))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(glass ? VecturTheme.borderStrong : VecturTheme.borderSubtle, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

struct VecturStatPill: View {
    let text: String
    var color: Color = VecturTheme.cyan

    var body: some View {
        Text(text)
            .font(.system(size: 10, weight: .heavy))
            .tracking(0.6)
            .textCase(.uppercase)
            .lineLimit(1)
            .fixedSize(horizontal: true, vertical: false)
            .foregroundStyle(color)
            .padding(.horizontal, 9)
            .padding(.vertical, 5)
            .background(color.opacity(0.15))
            .overlay(Capsule().stroke(color.opacity(0.42), lineWidth: 1))
            .clipShape(Capsule())
    }
}

struct VecturIconChip: View {
    let systemName: String
    let action: () -> Void
    var tint: Color = VecturTheme.textSecondary

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 48, height: 48)
                .background(VecturTheme.elevated.opacity(0.94))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(VecturTheme.borderSubtle, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct VecturBrandMark: View {
    var size: CGFloat = 88
    var pulsing = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.24, style: .continuous)
                .fill(VecturTheme.overlay)
                .overlay(
                    RoundedRectangle(cornerRadius: size * 0.24, style: .continuous)
                        .stroke(VecturTheme.cyan.opacity(0.7), lineWidth: 1)
                )
            Image(systemName: "bolt.fill")
                .font(.system(size: size * 0.48, weight: .bold))
                .foregroundStyle(.white)
        }
        .frame(width: size, height: size)
        .shadow(color: VecturTheme.cyan.opacity(0.18), radius: 20, y: 8)
    }
}

struct VecturSectionHeader: View {
    let title: String
    var trailing: String?

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .tracking(1.4)
                .foregroundStyle(VecturTheme.textMuted)
            Spacer()
            if let trailing {
                Text(trailing)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(VecturTheme.textDisabled)
            }
        }
        .textCase(.uppercase)
    }
}
