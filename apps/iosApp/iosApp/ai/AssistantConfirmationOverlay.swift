import SwiftUI

/// Full-screen confirmation popup shown when the AI assistant has resolved a
/// user request to a concrete destination.
///
/// Behaviour:
/// - The popup is the ONLY confirmation step. Tapping "Navigate now" jumps
///   straight to AR navigation and skips the manual route-preview screen.
/// - Tapping "Cancel" or the dimmed background dismisses the popup and
///   returns the user to the assistant input so they can refine their query.
/// - Up to two alternative candidates are listed underneath the suggested
///   one when the agent returns multiple matches.
struct AssistantConfirmationOverlay: View {
    let result: AiRouteAgentResult
    let onConfirm: () -> Void
    let onPickAlternative: (AiRouteCandidate) -> Void
    let onCancel: () -> Void

    @State private var appeared = false

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture { onCancel() }

            VStack(spacing: 0) {
                Spacer(minLength: 24)
                card
                    .padding(.horizontal, 24)
                Spacer(minLength: 24)
            }
        }
        .opacity(appeared ? 1 : 0)
        .scaleEffect(appeared ? 1 : 0.92)
        .onAppear {
            withAnimation(.spring(response: 0.32, dampingFraction: 0.78)) {
                appeared = true
            }
        }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: 18) {
            header

            assistantBubble(result.assistantMessage)

            if let selected = result.selectedDestination {
                CandidateRow(candidate: selected, isPrimary: true) { onConfirm() }
            }

            let alternatives = result.candidateDestinations
                .filter { $0.id != result.selectedDestination?.id }
                .prefix(2)
            if !alternatives.isEmpty {
                Text("Other options")
                    .font(.caption.weight(.bold))
                    .tracking(1.2)
                    .foregroundStyle(VecturTheme.textMuted)
                    .textCase(.uppercase)
                ForEach(Array(alternatives), id: \.id) { candidate in
                    CandidateRow(candidate: candidate, isPrimary: false) {
                        onPickAlternative(candidate)
                    }
                }
            }

            HStack(spacing: 10) {
                Button(action: onCancel) {
                    Text("Cancel")
                        .vecturSecondaryButton()
                }
                .buttonStyle(.plain)
                .frame(width: 110)

                Button(action: onConfirm) {
                    Label("Navigate now", systemImage: "arrow.triangle.turn.up.right.circle.fill")
                        .vecturPrimaryButton()
                }
                .buttonStyle(.plain)
            }
        }
        .padding(22)
        .background(VecturTheme.card)
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(VecturTheme.cyan.opacity(0.5), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: VecturTheme.cyan.opacity(0.18), radius: 24, y: 12)
    }

    private var header: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(VecturTheme.cyan.opacity(0.18))
                    .frame(width: 44, height: 44)
                Image(systemName: "sparkles")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(VecturTheme.cyan)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("Found it!")
                    .font(.title3.weight(.heavy))
                    .foregroundStyle(VecturTheme.textPrimary)
                if let summary = result.routeSummary {
                    Text(summary)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(VecturTheme.textMuted)
                }
            }
            Spacer()
            Button(action: onCancel) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundStyle(VecturTheme.textSecondary)
                    .frame(width: 32, height: 32)
                    .background(VecturTheme.elevated)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
        }
    }

    private func assistantBubble(_ text: String) -> some View {
        Text(text)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(VecturTheme.textPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(VecturTheme.elevated.opacity(0.85))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct CandidateRow: View {
    let candidate: AiRouteCandidate
    let isPrimary: Bool
    let onPick: () -> Void

    var body: some View {
        Button(action: onPick) {
            HStack(spacing: 12) {
                Image(systemName: VecturTheme.categoryIcon(candidate.category))
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(VecturTheme.categoryColor(candidate.category))
                    .frame(width: 42, height: 42)
                    .background(VecturTheme.categoryColor(candidate.category).opacity(0.16))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 4) {
                    if isPrimary {
                        VecturStatPill(text: "Suggested", color: VecturTheme.cyan)
                    }
                    Text(candidate.displayName)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(VecturTheme.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                    Text(candidate.summary)
                        .font(.caption)
                        .foregroundStyle(VecturTheme.textMuted)
                        .lineLimit(2)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(VecturTheme.textDisabled)
            }
            .padding(12)
            .background(isPrimary ? VecturTheme.cyan.opacity(0.12) : VecturTheme.elevated.opacity(0.9))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(isPrimary ? VecturTheme.cyan.opacity(0.55) : VecturTheme.borderSubtle, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
