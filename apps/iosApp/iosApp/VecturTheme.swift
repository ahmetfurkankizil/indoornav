import SwiftUI

/// Lightweight style namespace for consistent visitor-facing UI.
/// Not a full design system — just shared constants to reduce inline duplication.
enum VecturTheme {

    // MARK: - Colors & Gradients

    static let accentGradient = LinearGradient(
        colors: [.blue, .blue.opacity(0.8)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let cardBackground = Color(.systemGray6)

    // MARK: - View Modifiers

    struct PrimaryButton: ViewModifier {
        func body(content: Content) -> some View {
            content
                .font(.headline)
                .padding()
                .frame(maxWidth: .infinity)
                .background(accentGradient)
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .shadow(color: .blue.opacity(0.25), radius: 8, y: 4)
        }
    }

    struct SecondaryButton: ViewModifier {
        func body(content: Content) -> some View {
            content
                .font(.subheadline.weight(.medium))
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color(.systemGray5))
                .foregroundColor(.primary)
                .clipShape(RoundedRectangle(cornerRadius: 10))
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
