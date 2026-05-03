package com.vecturai.android.ar

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.vecturai.android.VecturaiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Frame
import com.vecturai.android.DeviceEnvironment
import com.vecturai.android.navigation.ArCameraFlowViewModel
import com.vecturai.android.ui.ArNavigationScreen
import com.vecturai.android.ui.DestinationSelectScreen
import com.vecturai.android.ui.EntranceConfirmedSheet
import com.vecturai.android.ui.QRScanScreen
import com.vecturai.android.ui.RoutePreviewScreen
import com.vecturai.designsystem.Spacing
import com.vecturai.designsystem.VecturaiCard
import com.vecturai.designsystem.VecturaiColors
import com.vecturai.designsystem.VecturaiHapticsGate
import com.vecturai.designsystem.VecturaiPrimaryButton
import com.vecturai.designsystem.VecturaiSecondaryButton
import com.vecturai.designsystem.VecturaiShapes
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
    private val isEmulator by lazy { DeviceEnvironment.isLikelyEmulator() }

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
            routeRenderer = arViewModel.routeRenderer,
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
                VecturaiHapticsGate(enabled = AndroidHapticManager.HapticsEnabled) {
                    ArCameraContent()
                }
            }
        }
    }

    private suspend fun downloadMarkerImage(token: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = "${VecturaiConfig.API_BASE_URL}/mobile/buildings/$token/qr"
            val bytes = java.net.URL(url).readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            println("[ARDiag] Failed to download marker: ${e.message}")
            null
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
                    onSimulateScan = if (isEmulator) ::simulateEntranceScan else null,
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
                    println("[ArActivity] Phase.ArNavigation: routePackage=${routePackage != null}, originRoom=${session.selectedOriginRoom?.id ?: "null"}")
                    if (routePackage != null) {
                        LaunchedEffect(routePackage, session.validatedEntranceMarker, session.selectedOriginRoom) {
                            println("[ArActivity] LaunchedEffect configure: originRoom=${session.selectedOriginRoom?.id ?: "null"}")
                            arViewModel.configure(routePackage, session.validatedEntranceMarker, session.selectedOriginRoom)
                            
                            // Alignment is now handled automatically in ViewModel.onFrame when TRACKING is ready
                        }
                        ArNavigationScreen(
                            viewModel = arViewModel,
                            isEmulator = isEmulator,
                            onEnd = flowModel::goBackToDestinationSelect,
                            onRetryActivity = ::recreate,
                            onNavigateElsewhere = ::navigateElsewhere,
                        )
                    }
                }
                is ArCameraFlowViewModel.Phase.FatalError -> FatalErrorOverlay(
                    message = currentPhase.message,
                    onClose = flowModel::goBackToDestinationSelect,
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
            
            // Start the AR session immediately so we can scan QR codes.
            // We don't wait for reviewedConfig anymore because it's fetched AFTER the scan.
            unifiedSession.onActivityResume(
                activity = this@ArCameraActivity,
                cameraTextureId = textureId,
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

    private fun navigateElsewhere() {
        arViewModel.endNavigation()
        flowModel.goBackToDestinationSelect()
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
            .background(VecturaiColors.SurfaceCanvas.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        VecturaiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Preparing camera...",
                    color = VecturaiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
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
            .background(VecturaiColors.SurfaceCanvas.copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        VecturaiCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(VecturaiShapes.Large)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f), VecturaiShapes.Large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Camera permission",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Camera access needed",
                    color = VecturaiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Camera access lets VecturAI scan the entrance code and show AR guidance.",
                    textAlign = TextAlign.Center,
                    color = VecturaiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VecturaiPrimaryButton(text = "Grant Access", onClick = onGrant)
                VecturaiSecondaryButton(text = "Cancel", onClick = onCancel)
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
            .background(VecturaiColors.SurfaceCanvas.copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        VecturaiCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(VecturaiColors.AccentAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Unable to start",
                        modifier = Modifier.size(32.dp),
                        tint = VecturaiColors.AccentAmber,
                    )
                }
                Text(
                    "Unable to start",
                    color = VecturaiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    color = VecturaiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(Spacing.xxs))
                VecturaiPrimaryButton(text = "Retry", onClick = onRetry)
                VecturaiSecondaryButton(text = "Go Back", onClick = onClose)
            }
        }
    }
}
