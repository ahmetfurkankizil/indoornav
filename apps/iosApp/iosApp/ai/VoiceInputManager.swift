import Foundation
import AVFoundation
import Speech

/// Lightweight speech-to-text helper for the AI route assistant.
///
/// Wraps `SFSpeechRecognizer` + an `AVAudioEngine` tap. Designed to never
/// throw out of the demo: when permission is missing, the network is offline,
/// or speech recognition is unavailable on the device, this manager surfaces
/// a friendly error and the UI falls back to text input.
@MainActor
final class VoiceInputManager: NSObject, ObservableObject {

    // MARK: - Published state

    enum State: Equatable {
        case idle
        case requestingPermission
        case listening
        case finished(transcript: String)
        case unavailable(reason: String)
        case error(message: String)
    }

    @Published private(set) var state: State = .idle
    @Published private(set) var partialTranscript: String = ""

    // MARK: - Internals

    private var recognizer: SFSpeechRecognizer?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private let audioEngine = AVAudioEngine()

    override init() {
        super.init()
        // English-only for the demo, per spec.
        self.recognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
    }

    // MARK: - Public API

    var isAvailable: Bool {
        recognizer?.isAvailable == true
    }

    func startListening() {
        guard isAvailable else {
            state = .unavailable(reason: "Speech recognition isn't available on this device.")
            return
        }
        state = .requestingPermission

        SFSpeechRecognizer.requestAuthorization { [weak self] authStatus in
            DispatchQueue.main.async {
                guard let self else { return }
                guard authStatus == .authorized else {
                    self.state = .unavailable(reason: "Speech recognition permission was denied.")
                    return
                }
                AVAudioApplication.requestRecordPermission { granted in
                    DispatchQueue.main.async {
                        guard granted else {
                            self.state = .unavailable(reason: "Microphone permission was denied.")
                            return
                        }
                        self.beginRecognition()
                    }
                }
            }
        }
    }

    func stopListening(commitPartial: Bool = true) {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionRequest = nil
        recognitionTask?.finish()
        recognitionTask = nil

        if commitPartial {
            let trimmed = partialTranscript.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                state = .finished(transcript: trimmed)
                return
            }
        }
        state = .idle
    }

    func reset() {
        partialTranscript = ""
        state = .idle
    }

    // MARK: - Internals

    private func beginRecognition() {
        recognitionTask?.cancel()
        recognitionTask = nil

        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.record, mode: .measurement, options: .duckOthers)
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            state = .error(message: "Couldn't start audio session: \(error.localizedDescription)")
            return
        }

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        recognitionRequest = request

        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.removeTap(onBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }

        audioEngine.prepare()
        do {
            try audioEngine.start()
        } catch {
            state = .error(message: "Couldn't start microphone: \(error.localizedDescription)")
            return
        }

        partialTranscript = ""
        state = .listening

        recognitionTask = recognizer?.recognitionTask(with: request) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let result {
                    self.partialTranscript = result.bestTranscription.formattedString
                    if result.isFinal {
                        self.stopListening(commitPartial: true)
                    }
                }
                if let error {
                    let nsError = error as NSError
                    // Cancellation produces "kAFAssistantErrorDomain Code=216" — treat as a normal stop.
                    if nsError.code == 216 || nsError.localizedDescription.contains("cancelled") {
                        return
                    }
                    self.audioEngine.stop()
                    self.audioEngine.inputNode.removeTap(onBus: 0)
                    self.recognitionRequest = nil
                    self.recognitionTask = nil
                    if !self.partialTranscript.isEmpty {
                        self.state = .finished(transcript: self.partialTranscript)
                    } else {
                        self.state = .error(message: error.localizedDescription)
                    }
                }
            }
        }
    }
}
