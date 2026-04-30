package com.vecturai.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vecturai.android.navigation.ArCameraFlowViewModel

@Composable
fun QRScanScreen(
    flowModel: ArCameraFlowViewModel,
    onCancel: () -> Unit,
    onSimulateScan: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18).copy(alpha = 0.92f))
    ) {
        ScanDotBackground()
        QRScanChrome(
            flowModel = flowModel,
            onRetry = {
                flowModel.clearQRError()
            },
            onCancel = onCancel,
            onSimulateScan = onSimulateScan,
        )
    }
}

@Composable
private fun QRScanChrome(
    flowModel: ArCameraFlowViewModel,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSimulateScan: (() -> Unit)?,
) {
    val error by flowModel.qrError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF121A28))
                    .border(1.dp, Color(0xFF24334A), RoundedCornerShape(13.dp))
                    .clickable(role = Role.Button, onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF168BFF),
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Scanning...",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 24.sp,
                )
                Text(
                    "Find the entrance poster",
                    color = Color(0xFF7F8A9D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(240.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF071C33).copy(alpha = 0.48f))
                .border(2.dp, Color(0xFF168BFF), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(184.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF2C96FF).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            )
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color(0xFF168BFF),
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (error == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = Color(0xFF168BFF),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Looking for entrance code",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Point your camera at the entrance poster",
                                color = Color(0xFF8A95A8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                    if (onSimulateScan != null) {
                        Button(
                            onClick = onSimulateScan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                        ) {
                            Text("Simulate Entrance Scan", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFF3B2A08)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Code not recognized",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                error.orEmpty(),
                                color = Color(0xFF8A95A8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                    ) {
                        Text("Try Again", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanDotBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val spacing = 22.dp.toPx()
        val radius = 0.85.dp.toPx()
        var x = 10.dp.toPx()
        while (x < size.width) {
            var y = 14.dp.toPx()
            while (y < size.height) {
                drawCircle(
                    color = Color(0xFF263750).copy(alpha = 0.36f),
                    radius = radius,
                    center = Offset(x, y),
                )
                y += spacing
            }
            x += spacing
        }
    }
}
