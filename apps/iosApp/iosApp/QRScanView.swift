import SwiftUI
import AVFoundation

/// QR code scanning screen with real camera-backed AVCaptureSession scanner.
///
/// Phase 4: Uses AVFoundation to scan QR codes from the live camera feed.
/// Validates the QR payload against the reviewed package before proceeding.
/// Falls back to a simulated scan button on Simulator (no camera available).
struct QRScanView: View {
    @ObservedObject var flow: NavigationFlowModel

    @State private var scanError: String?
    @State private var isProcessing = false
    /// Incremented on retry to force a new QRCameraPreview (and fresh AVCaptureSession).
    @State private var scanAttempt = 0

    var body: some View {
        ZStack {
            // Live camera preview (or black on simulator)
            #if targetEnvironment(simulator)
            Color.black.ignoresSafeArea()
            #else
            QRCameraPreview(onCodeScanned: handleScannedCode)
                .id(scanAttempt) // force new VC + fresh camera session on retry
                .ignoresSafeArea()
            #endif

            // Overlay UI
            VStack(spacing: 32) {
                Spacer()

                // Viewfinder frame
                ZStack {
                    RoundedRectangle(cornerRadius: 20)
                        .strokeBorder(.white.opacity(0.6), lineWidth: 2)
                        .frame(width: 260, height: 260)

                    if isProcessing {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .tint(.white)
                            .scaleEffect(1.5)
                    } else {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 80))
                            .foregroundStyle(.white.opacity(0.7))
                    }
                }

                Text(isProcessing ? "Validating..." : "Point camera at entrance QR code")
                    .font(.title3)
                    .foregroundStyle(.white)

                // Error display
                if let error = scanError {
                    VStack(spacing: 8) {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(.orange)
                            Text(error)
                                .font(.subheadline)
                                .foregroundStyle(.white)
                                .multilineTextAlignment(.center)
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(.red.opacity(0.3))
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                        Button(action: { scanError = nil; isProcessing = false; scanAttempt += 1 }) {
                            Text("Try Again")
                                .font(.callout).fontWeight(.medium)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 20).padding(.vertical, 10)
                                .background(.blue.opacity(0.85))
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 24)
                }

                // Simulated scan for Simulator / demo fallback
                #if targetEnvironment(simulator)
                if !isProcessing && scanError == nil {
                    Button(action: simulateScan) {
                        Label("Simulate Entrance Scan", systemImage: "qrcode")
                            .font(.headline)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(.blue)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.horizontal, 40)

                    Text("Simulator — no camera available")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.5))
                }
                #endif

                Spacer()
            }

            // Close button
            VStack {
                HStack {
                    Spacer()
                    Button(action: { flow.endNavigation() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundStyle(.white.opacity(0.8))
                    }
                    .padding()
                }
                Spacer()
            }
        }
    }

    // MARK: - QR Handling

    private func handleScannedCode(_ raw: String) {
        guard !isProcessing else { return }
        isProcessing = true
        scanError = nil

        Task {
            let error = await flow.handleQRScanned(raw)
            isProcessing = false
            if let error {
                scanError = error
            }
        }
    }

    #if targetEnvironment(simulator)
    private func simulateScan() {
        // Paste a real QR token here to test remote fetch on Simulator,
        // or keep the v1 bundled format for the local demo building.
        let demoPayload = """
        {"type":"vecturai-entrance","buildingId":"house-demo-01","entranceId":"marker-entrance-a","v":1}
        """
        handleScannedCode(demoPayload)
    }
    #endif
}

// MARK: - Camera Preview (UIViewRepresentable)

#if !targetEnvironment(simulator)
/// UIKit-backed camera preview that scans for QR codes via AVCaptureSession.
struct QRCameraPreview: UIViewControllerRepresentable {
    let onCodeScanned: (String) -> Void

    func makeUIViewController(context: Context) -> QRScannerViewController {
        let vc = QRScannerViewController()
        vc.onCodeScanned = onCodeScanned
        return vc
    }

    func updateUIViewController(_ uiViewController: QRScannerViewController, context: Context) {}
}

/// Minimal AVCaptureSession-based QR scanner view controller.
class QRScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCodeScanned: ((String) -> Void)?

    private let captureSession = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var hasReported = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        setupCamera()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        hasReported = false
        if !captureSession.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.captureSession.startRunning()
            }
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        tearDownCamera()
    }

    /// Fully release the camera so ARKit can acquire it without contention.
    private func tearDownCamera() {
        if captureSession.isRunning {
            captureSession.stopRunning()
        }
        for input in captureSession.inputs {
            captureSession.removeInput(input)
        }
        for output in captureSession.outputs {
            captureSession.removeOutput(output)
        }
    }

    private func setupCamera() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else {
            print("[QRScanner] Camera not available")
            return
        }

        if captureSession.canAddInput(input) {
            captureSession.addInput(input)
        }

        let output = AVCaptureMetadataOutput()
        if captureSession.canAddOutput(output) {
            captureSession.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.qr]
        }

        let layer = AVCaptureVideoPreviewLayer(session: captureSession)
        layer.videoGravity = .resizeAspectFill
        layer.frame = view.bounds
        view.layer.addSublayer(layer)
        self.previewLayer = layer

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession.startRunning()
        }
    }

    // MARK: - AVCaptureMetadataOutputObjectsDelegate

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard !hasReported,
              let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              object.type == .qr,
              let value = object.stringValue else { return }

        hasReported = true
        tearDownCamera()
        onCodeScanned?(value)
    }
}
#endif
