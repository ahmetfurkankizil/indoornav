package com.vecturai.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vecturai.android.ar.ArSessionManager
import com.vecturai.android.ar.ArFeatureFlags
import com.vecturai.android.navigation.AndroidNavigationFlowModel
import java.util.concurrent.atomic.AtomicBoolean
import androidx.camera.core.CameraState
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QRScanScreen(flowModel: AndroidNavigationFlowModel) {
    val context = LocalContext.current
    var retryToken by remember { mutableStateOf(0) }
    var isReleasing by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            if (ArFeatureFlags.ArUnifiedCameraPipeline) {
                ArCoreQrPreview(
                    sessionManager = flowModel.sessionManager,
                    retryToken = retryToken,
                    onCodeScanned = flowModel::onQRScanned,
                    onReleasing = { isReleasing = true }
                )
            } else {
                CameraQrPreview(
                    retryToken = retryToken,
                    onCodeScanned = flowModel::onQRScanned,
                    onReleasing = { isReleasing = true }
                )
            }
        } else {
            CameraPermissionCard(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
        }

        if (isReleasing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text("Releasing camera...", color = Color.White)
                }
            }
        } else {
            QRScanChrome(
                flowModel = flowModel,
                onRetry = {
                    flowModel.clearQRError()
                    retryToken++
                },
            )
        }
    }
}

@Composable
private fun QRScanChrome(flowModel: AndroidNavigationFlowModel, onRetry: () -> Unit) {
    val error by flowModel.qrError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = flowModel::endNavigation) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text("Scanning...", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Transparent)
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(3.dp, Color.White, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.weight(1f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (error == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text("Looking for entrance code", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Point your camera at the entrance poster",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Code not recognized", fontWeight = FontWeight.SemiBold)
                            Text(
                                error.orEmpty(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionCard(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(48.dp))
                Text("Camera Access Needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Camera access lets VecturAI scan the entrance code.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreview(
    retryToken: Int,
    onCodeScanned: (String) -> Unit,
    onReleasing: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    val locked = remember { AtomicBoolean(false) }
    val processing = remember { AtomicBoolean(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(retryToken) {
        locked.set(false)
        processing.set(false)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener(
                {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    var boundCamera: androidx.camera.core.Camera? = null

                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        if (locked.get() || !processing.compareAndSet(false, true)) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            processing.set(false)
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        var scannedRaw: String? = null
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                if (raw != null && locked.compareAndSet(false, true)) {
                                    scannedRaw = raw
                                }
                            }
                            .addOnCompleteListener {
                                processing.set(false)
                                imageProxy.close()
                                val raw = scannedRaw
                                if (raw != null) {
                                    println("[QRScan] Code detected, releasing camera...")
                                    onReleasing()
                                    analysis.clearAnalyzer()
                                    scanner.close()
                                    provider.unbindAll()
                                    
                                    val cameraInfo = boundCamera?.cameraInfo
                                    if (cameraInfo != null) {
                                        lifecycleOwner.lifecycleScope.launch {
                                            var isClosed = false
                                            val observer = Observer<CameraState> { state ->
                                                if (state.type == CameraState.Type.CLOSED) {
                                                    isClosed = true
                                                }
                                            }
                                            cameraInfo.cameraState.observe(lifecycleOwner, observer)
                                            val start = System.currentTimeMillis()
                                            while (!isClosed && System.currentTimeMillis() - start < 3000L) {
                                                delay(50)
                                            }
                                            cameraInfo.cameraState.removeObserver(observer)
                                            println("[QRScan] Notifying flow model after release...")
                                            onCodeScanned(raw)
                                        }
                                    } else {
                                        println("[QRScan] Fallback notifying flow model...")
                                        onCodeScanned(raw)
                                    }
                                }
                            }
                    }

                    provider.unbindAll()
                    boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                },
                ContextCompat.getMainExecutor(ctx),
            )

            previewView
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.addListener(
                { cameraProviderFuture.get().unbindAll() },
                ContextCompat.getMainExecutor(context),
            )
            scanner.close()
        }
    }
}

@Composable
private fun ArCoreQrPreview(
    sessionManager: ArSessionManager,
    retryToken: Int,
    onCodeScanned: (String) -> Unit,
    onReleasing: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    val locked = remember { AtomicBoolean(false) }
    val processing = remember { AtomicBoolean(false) }

    LaunchedEffect(retryToken) {
        locked.set(false)
        processing.set(false)
        sessionManager.createSessionWithoutMarker(activity)
        sessionManager.resumeSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!ArFeatureFlags.ArUnifiedCameraPipeline) {
                sessionManager.stopSession()
            }
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            android.opengl.GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                preserveEGLContextOnPause = true
                setRenderer(object : android.opengl.GLSurfaceView.Renderer {
                    var textureId = 0
                    var textureBoundSession: com.google.ar.core.Session? = null

                    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
                        android.opengl.GLES20.glClearColor(0f, 0f, 0f, 1f)
                        val textures = IntArray(1)
                        android.opengl.GLES20.glGenTextures(1, textures, 0)
                        textureId = textures[0]
                        android.opengl.GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
                    }

                    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
                        android.opengl.GLES20.glViewport(0, 0, width, height)
                    }

                    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)
                        val session = sessionManager.session ?: return
                        
                        if (textureBoundSession !== session) {
                            sessionManager.setCameraTexture(textureId)
                            textureBoundSession = session
                        }

                        try {
                            val frame = session.update()
                            if (locked.get() || !processing.compareAndSet(false, true)) return
                            
                            val image = try {
                                frame.acquireCameraImage()
                            } catch (e: Exception) {
                                processing.set(false)
                                null
                            } ?: return
                            
                            val windowManager = activity.getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager
                            val rotation = windowManager?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0
                            val degrees = when (rotation) {
                                android.view.Surface.ROTATION_0 -> 90
                                android.view.Surface.ROTATION_90 -> 0
                                android.view.Surface.ROTATION_180 -> 270
                                android.view.Surface.ROTATION_270 -> 180
                                else -> 90
                            }

                            val inputImage = InputImage.fromMediaImage(image, degrees)
                            var scannedRaw: String? = null
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val raw = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                    if (raw != null && locked.compareAndSet(false, true)) {
                                        scannedRaw = raw
                                    }
                                }
                                .addOnCompleteListener {
                                    image.close()
                                    processing.set(false)
                                    val raw = scannedRaw
                                    if (raw != null) {
                                        onReleasing()
                                        onCodeScanned(raw)
                                    }
                                }
                        } catch (t: Throwable) {
                            processing.set(false)
                        }
                    }
                })
                renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
    )
}
