package com.vecturai.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
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
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
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
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp),
            )
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
                    if (onSimulateScan != null) {
                        Button(onClick = onSimulateScan, modifier = Modifier.fillMaxWidth()) {
                            Text("Simulate Entrance Scan")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(28.dp),
                        )
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
