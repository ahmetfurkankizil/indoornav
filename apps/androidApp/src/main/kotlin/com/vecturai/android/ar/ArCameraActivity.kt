package com.vecturai.android.ar

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Frame
import com.vecturai.android.navigation.ArCameraFlowViewModel
import com.vecturai.android.ui.ArNavigationScreen
import com.vecturai.android.ui.DestinationSelectScreen
import com.vecturai.android.ui.EntranceConfirmedSheet
import com.vecturai.android.ui.QRScanScreen
import com.vecturai.android.ui.RoutePreviewScreen
import com.vecturai.designsystem.VecturaiTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArCameraActivity : ComponentActivity() {
    private val flowModel: ArCameraFlowViewModel by viewModel()
    private val arViewModel: AndroidArNavigationViewModel by viewModel()

    private val unifiedSession = UnifiedArSession()
    private val textureReady = CompletableDeferred<Int>()
    private val showPermissionOverlay = mutableStateOf(false)
    private var glSurfaceView: GLSurfaceView? = null
    private var resumeJob: Job? = null
    private var cameraTextureId = 0
    private var askedForPermission = false
    private val isEmulator by lazy { isLikelyEmulator() }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        showPermissionOverlay.value = !granted
        if (granted) {
            resumeCameraWhenReady()
        }
    }

    private val renderer by lazy {
        UnifiedArRenderer(
            activity = this,
            unifiedSession = unifiedSession,
            onTextureCreated = { textureId ->
                cameraTextureId = textureId
                if (!textureReady.isCompleted) {
                    textureReady.complete(textureId)
                }
                runOnUiThread { resumeCameraWhenReady() }
            },
            onFrame = ::onArFrame,
            onFatalFailure = ::finishWithCameraError,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VecturaiTheme {
                ArCameraContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isEmulator) {
            showPermissionOverlay.value = false
            return
        }
        glSurfaceView?.onResume()
        if (hasCameraPermission()) {
            showPermissionOverlay.value = false
            resumeCameraWhenReady()
        } else {
            showPermissionOverlay.value = true
            if (!askedForPermission) {
                askedForPermission = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onPause() {
        resumeJob?.cancel()
        unifiedSession.onActivityPause()
        glSurfaceView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        unifiedSession.onActivityDestroy()
        super.onDestroy()
    }

    @Composable
    private fun ArCameraContent() {
        val phase by flowModel.phase.collectAsState()
        val session by flowModel.session.collectAsState()
        val needsPermission by showPermissionOverlay

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (!isEmulator) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        GLSurfaceView(context).apply {
                            setEGLContextClientVersion(2)
                            preserveEGLContextOnPause = true
                            setRenderer(renderer)
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            glSurfaceView = this
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                onResume()
                            }
                        }
                    },
                )
            }

            when (val currentPhase = phase) {
                ArCameraFlowViewModel.Phase.Loading -> LoadingOverlay()
                ArCameraFlowViewModel.Phase.QrScan -> QRScanScreen(
                    flowModel = flowModel,
                    onCancel = ::finishFlow,
                    onSimulateScan = ::simulateEntranceScan,
                )
                is ArCameraFlowViewModel.Phase.EntranceConfirmed -> EntranceConfirmedSheet(
                    entranceName = currentPhase.entranceName,
                    onContinue = flowModel::proceedToDestinationSelect,
                )
                ArCameraFlowViewModel.Phase.DestinationSelect -> DestinationSelectScreen(
                    flowModel = flowModel,
                    onCancel = ::finishFlow,
                )
                ArCameraFlowViewModel.Phase.RoutePreview -> RoutePreviewScreen(flowModel)
                ArCameraFlowViewModel.Phase.ArNavigation -> {
                    val routePackage = session.routePackage
                    if (routePackage != null) {
                        LaunchedEffect(routePackage, session.validatedEntranceMarker) {
                            arViewModel.configure(routePackage, session.validatedEntranceMarker)
                            if (isEmulator) {
                                arViewModel.simulateAlignment()
                            }
                        }
                        ArNavigationScreen(
                            viewModel = arViewModel,
                            onEnd = ::finishFlow,
                            onRetryActivity = ::recreate,
                        )
                    }
                }
                is ArCameraFlowViewModel.Phase.FatalError -> FatalErrorOverlay(
                    message = currentPhase.message,
                    onClose = ::finishFlow,
                    onRetry = ::recreate,
                )
            }

            if (needsPermission) {
                CameraPermissionOverlay(
                    onGrant = {
                        askedForPermission = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onCancel = ::finishFlow,
                )
            }
        }
    }

    private fun resumeCameraWhenReady() {
        if (isEmulator) {
            return
        }
        if (!hasCameraPermission()) {
            showPermissionOverlay.value = true
            return
        }
        resumeJob?.cancel()
        resumeJob = lifecycleScope.launch {
            val textureId = if (cameraTextureId != 0) cameraTextureId else textureReady.await()
            flowModel.session
                .map { it.reviewedConfig }
                .filterNotNull()
                .first()
            unifiedSession.onActivityResume(
                activity = this@ArCameraActivity,
                cameraTextureId = textureId,
                marker = flowModel.entranceMarkerForSession(),
            )
        }
    }

    private fun simulateEntranceScan() {
        flowModel.onQRScanned(DEMO_QR_PAYLOAD)
    }

    private fun onArFrame(frame: Frame, width: Int, height: Int, rotationDegrees: Int) {
        when (flowModel.phase.value) {
            ArCameraFlowViewModel.Phase.QrScan -> flowModel.onQrFrame(frame, rotationDegrees)
            ArCameraFlowViewModel.Phase.ArNavigation -> arViewModel.onFrame(frame, width, height)
            else -> Unit
        }
    }

    private fun finishFlow() {
        arViewModel.endNavigation()
        finish()
    }

    private fun finishWithCameraError(error: Throwable) {
        val type = error::class.java.simpleName.ifBlank { error::class.java.name }
        val message = error.message
        val detail = if (message.isNullOrBlank() || message == "null") type else "$type: $message"
        runOnUiThread {
            Toast.makeText(
                this,
                "AR camera stopped. Reopen navigation to start a fresh session. ($detail)",
                Toast.LENGTH_LONG,
            ).show()
            finish()
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun isLikelyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val bootloader = Build.BOOTLOADER.lowercase()
        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("sdk_gphone") ||
            fingerprint.contains("sdk_phone") ||
            model.contains("google_sdk") ||
            model.contains("sdk_gphone") ||
            model.contains("sdk phone") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            board.contains("goldfish") ||
            board.contains("ranchu") ||
            bootloader.contains("unknown") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product == "google_sdk" ||
            product.contains("sdk_gphone") ||
            product.contains("sdk_phone") ||
            product.contains("emulator") ||
            product.contains("vbox")
    }

    private companion object {
        const val DEMO_QR_PAYLOAD =
            """{"type":"vecturai-entrance","buildingId":"19","entranceId":"marker-main-entrance","v":1}"""
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18).copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = Color(0xFF168BFF), modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Preparing camera...",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionOverlay(
    onGrant: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18).copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF071C33))
                        .border(1.dp, Color(0xFF0B60AE), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color(0xFF168BFF))
                }
                Text(
                    "Camera access needed",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Camera access lets VecturAI scan the entrance code and show AR guidance.",
                    textAlign = TextAlign.Center,
                    color = Color(0xFFB6BFCE),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Button(
                    onClick = onGrant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                ) {
                    Text("Grant Access", fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(role = Role.Button, onClick = onCancel),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF121A28),
                    border = BorderStroke(1.dp, Color(0xFF2B3952)),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = Color(0xFFB6BFCE), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FatalErrorOverlay(
    message: String,
    onClose: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18).copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B2A08)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color(0xFFF59E0B))
                }
                Text(
                    "Unable to start",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFB6BFCE),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                ) {
                    Text("Retry", fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(role = Role.Button, onClick = onClose),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF121A28),
                    border = BorderStroke(1.dp, Color(0xFF2B3952)),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Go Back", color = Color(0xFFB6BFCE), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
