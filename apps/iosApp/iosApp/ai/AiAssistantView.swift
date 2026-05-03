import SwiftUI

/// AI route-selection assistant card embedded inside `DestinationSelectView`.
///
/// Renders:
/// - A conversational text input + microphone button
/// - The assistant's response message
/// - The selected destination confirmation card
/// - Up to two alternative candidates the user can pick instead
/// - "Start route" / "Cancel" / "Search manually" actions
///
/// Voice input is best-effort: when speech recognition or microphone
/// permission is unavailable the mic button surfaces a hint and the text
/// input keeps working unaffected.
struct AiAssistantView: View {

    @ObservedObject var viewModel: AiAssistantViewModel
    @StateObject private var voice = VoiceInputManager()
    @FocusState private var inputFocused: Bool

    var body: some View {
        VStack(spacing: 14) {
            assistantHeader
            inputRow

            switch viewModel.status {
            case .idle:
                idleHint
            case .thinking:
                thinkingRow
            case .awaitingConfirmation:
                // Confirmation is shown as a full-screen overlay by the
                // parent view. We deliberately do NOT render it inline so
                // there's only one confirmation surface on screen.
                EmptyView()
            case .noMatch(let message):
                noMatchCard(message: message)
            }

            if !voice.partialTranscript.isEmpty || voiceErrorText != nil {
                voiceStatusRow
            }
        }
        .padding(16)
        .background(VecturTheme.card.opacity(0.96))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(VecturTheme.cyan.opacity(0.4), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .onChange(of: voice.state) { _, newState in
            handleVoiceStateChange(newState)
        }
    }

    // MARK: - Header

    private var assistantHeader: some View {
        HStack(spacing: 10) {
            Image(systemName: "sparkles")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(VecturTheme.cyan)
                .frame(width: 32, height: 32)
                .background(VecturTheme.cyan.opacity(0.18))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            VStack(alignment: .leading, spacing: 2) {
                Text("Ask Vectura")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(VecturTheme.textPrimary)
                Text(viewModel.mode == .mock ? "Offline · always available" : "GPT-backed · falls back to offline")
                    .font(.caption)
                    .foregroundStyle(VecturTheme.textMuted)
            }
            Spacer()
            Menu {
                Picker("Backend", selection: $viewModel.mode) {
                    ForEach(AiRouteAgentMode.allCases) { mode in
                        Text(mode.displayName).tag(mode)
                    }
                }
                Toggle("Show dev trace", isOn: $viewModel.showDevTrace)
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(VecturTheme.textSecondary)
                    .frame(width: 32, height: 32)
            }
        }
    }

    // MARK: - Input row

    private var inputRow: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "text.bubble")
                    .foregroundStyle(VecturTheme.textMuted)
                TextField("Tell Vectura where you want to go...", text: $viewModel.inputText, axis: .vertical)
                    .lineLimit(1...3)
                    .focused($inputFocused)
                    .submitLabel(.send)
                    .onSubmit { viewModel.submit(inputMode: .text) }
                    .foregroundStyle(VecturTheme.textPrimary)
                if !viewModel.inputText.isEmpty {
                    Button {
                        viewModel.inputText = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(VecturTheme.textMuted)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 48)
            .background(VecturTheme.elevated.opacity(0.9))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(VecturTheme.borderSubtle, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            micButton
            sendButton
        }
    }

    private var micButton: some View {
        Button {
            switch voice.state {
            case .listening:
                voice.stopListening(commitPartial: true)
            default:
                voice.startListening()
            }
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isListening ? VecturTheme.red.opacity(0.85) : VecturTheme.elevated)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(isListening ? VecturTheme.red : VecturTheme.borderSubtle, lineWidth: 1)
                    )
                Image(systemName: isListening ? "waveform" : "mic.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(isListening ? .white : VecturTheme.cyan)
            }
            .frame(width: 48, height: 48)
        }
        .buttonStyle(.plain)
        .disabled(!voice.isAvailable && !isListening)
    }

    private var sendButton: some View {
        Button {
            viewModel.submit(inputMode: .text)
            inputFocused = false
        } label: {
            Image(systemName: "arrow.up")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
                .frame(width: 48, height: 48)
                .background(VecturTheme.primaryGradient)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        .opacity(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.55 : 1)
    }

    // MARK: - States

    private var idleHint: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Try saying:")
                .font(.caption.weight(.bold))
                .foregroundStyle(VecturTheme.textMuted)
                .textCase(.uppercase)
            HStack(spacing: 6) {
                ExampleChip(text: "I'm about to pee myself") { use($0) }
                ExampleChip(text: "I have a class in EA-Z04") { use($0) }
            }
            HStack(spacing: 6) {
                ExampleChip(text: "I really need to drink a coffee") { use($0) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var thinkingRow: some View {
        HStack(spacing: 10) {
            ProgressView()
            Text("Thinking...")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(VecturTheme.textSecondary)
            Spacer()
        }
        .padding(.vertical, 4)
    }

    private func noMatchCard(message: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            assistantBubble(text: message)
            HStack(spacing: 10) {
                Button { viewModel.cancel() } label: {
                    Text("Try again").vecturSecondaryButton()
                }
                .buttonStyle(.plain)
                Button {
                    viewModel.cancel()
                    inputFocused = true
                } label: {
                    Label("Search manually", systemImage: "magnifyingglass")
                        .vecturPrimaryButton()
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Voice status

    private var voiceStatusRow: some View {
        HStack(spacing: 10) {
            Image(systemName: isListening ? "waveform.circle.fill" : "exclamationmark.circle.fill")
                .foregroundStyle(isListening ? VecturTheme.red : VecturTheme.amber)
            VStack(alignment: .leading, spacing: 2) {
                if let err = voiceErrorText {
                    Text(err)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(VecturTheme.textSecondary)
                }
                if !voice.partialTranscript.isEmpty {
                    Text(voice.partialTranscript)
                        .font(.footnote)
                        .foregroundStyle(VecturTheme.textPrimary)
                        .lineLimit(2)
                }
            }
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(VecturTheme.elevated.opacity(0.85))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Helpers

    private func assistantBubble(text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "sparkles")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(VecturTheme.cyan)
                .frame(width: 22, height: 22)
                .background(VecturTheme.cyan.opacity(0.15))
                .clipShape(Circle())
            Text(text)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(VecturTheme.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .background(VecturTheme.elevated.opacity(0.85))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(VecturTheme.borderSubtle, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func use(_ phrase: String) {
        viewModel.inputText = phrase
        viewModel.submit(transcribed: phrase, inputMode: .text)
    }

    private var isListening: Bool {
        if case .listening = voice.state { return true }
        return false
    }

    private var voiceErrorText: String? {
        switch voice.state {
        case .unavailable(let reason): return reason
        case .error(let message): return message
        default: return nil
        }
    }

    private func handleVoiceStateChange(_ newState: VoiceInputManager.State) {
        if case .finished(let transcript) = newState {
            viewModel.inputText = transcript
            viewModel.submit(transcribed: transcript, inputMode: .voice)
            voice.reset()
        }
    }
}

// MARK: - Subviews

private struct ExampleChip: View {
    let text: String
    let action: (String) -> Void

    var body: some View {
        Button {
            action(text)
        } label: {
            Text("\u{201C}\(text)\u{201D}")
                .font(.caption.weight(.semibold))
                .foregroundStyle(VecturTheme.textSecondary)
                .lineLimit(1)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(VecturTheme.elevated.opacity(0.85))
                .overlay(Capsule().stroke(VecturTheme.borderSubtle, lineWidth: 1))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
