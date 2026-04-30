package com.example.vecturai.ar

import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class HostedCloudAnchor(
    val cloudAnchorId: String,
    val anchor: Anchor
)

object CloudAnchorHelper {
    suspend fun hostAnchor(
        session: Session,
        pose: Pose,
        ttlDays: Int = 1
    ): Result<HostedCloudAnchor> = suspendCancellableCoroutine { continuation ->
        var localAnchor: Anchor? = null
        runCatching {
            session.ensureCloudAnchorModeEnabled()
            val createdAnchor = session.createAnchor(pose)
            localAnchor = createdAnchor
            session.hostCloudAnchorAsync(createdAnchor, ttlDays) { cloudAnchorId, state ->
                if (!continuation.isActive) return@hostCloudAnchorAsync
                when (state) {
                    Anchor.CloudAnchorState.SUCCESS -> {
                        val hostedAnchor = localAnchor
                        if (hostedAnchor == null) {
                            continuation.resume(
                                Result.failure(IllegalStateException("Cloud Anchor host succeeded without a local anchor."))
                            )
                        } else {
                            continuation.resume(Result.success(HostedCloudAnchor(cloudAnchorId, hostedAnchor)))
                        }
                    }

                    else -> {
                        localAnchor?.detach()
                        continuation.resume(
                            Result.failure(IllegalStateException("Cloud Anchor host failed: $state"))
                        )
                    }
                }
            }
        }
            .onSuccess { future ->
                continuation.invokeOnCancellation {
                    future.cancel()
                    localAnchor?.detach()
                }
            }
            .onFailure { error ->
                localAnchor?.detach()
                if (continuation.isActive) {
                    continuation.resume(Result.failure(error))
                }
            }
        Unit
    }

    suspend fun resolveAnchor(
        session: Session,
        cloudAnchorId: String
    ): Result<Anchor> = suspendCancellableCoroutine { continuation ->
        runCatching {
            session.ensureCloudAnchorModeEnabled()
            session.resolveCloudAnchorAsync(cloudAnchorId) { anchor, state ->
                if (!continuation.isActive) return@resolveCloudAnchorAsync
                when (state) {
                    Anchor.CloudAnchorState.SUCCESS ->
                        continuation.resume(Result.success(anchor))

                    else -> {
                        anchor?.detach()
                        continuation.resume(
                            Result.failure(IllegalStateException("Cloud Anchor resolve failed: $state"))
                        )
                    }
                }
            }
        }
            .onSuccess { future ->
                continuation.invokeOnCancellation {
                    future.cancel()
                }
            }
            .onFailure { error ->
                if (continuation.isActive) {
                    continuation.resume(Result.failure(error))
                }
            }
        Unit
    }

    private fun Session.ensureCloudAnchorModeEnabled() {
        val currentConfig = config
        if (currentConfig.cloudAnchorMode != Config.CloudAnchorMode.ENABLED) {
            ArSessionConfig.configureIndoorCloudSession(this, currentConfig)
            configure(currentConfig)
        }
    }
}
