package com.example.vecturai.ar

import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.ResolveCloudAnchorFuture
import com.google.ar.core.Session
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
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
            assertCloudAnchorModeEnabled(session)
            val createdAnchor = session.createAnchor(pose)
            localAnchor = createdAnchor
            session.hostCloudAnchorAsync(createdAnchor, ttlDays) { cloudAnchorId, state ->
                if (!continuation.isActive) return@hostCloudAnchorAsync
                when (state) {
                    Anchor.CloudAnchorState.SUCCESS ->
                        continuation.resume(Result.success(HostedCloudAnchor(cloudAnchorId, createdAnchor)))

                    else -> {
                        createdAnchor.detach()
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
        cloudAnchorId: String,
        timeoutMs: Long
    ): Result<Anchor> {
        var future: ResolveCloudAnchorFuture? = null
        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    runCatching {
                        assertCloudAnchorModeEnabled(session)
                        session.resolveCloudAnchorAsync(cloudAnchorId) { anchor, state ->
                            if (!continuation.isActive) return@resolveCloudAnchorAsync
                            when (state) {
                                Anchor.CloudAnchorState.SUCCESS ->
                                    continuation.resume(Result.success(anchor))

                                else -> {
                                    anchor.detach()
                                    continuation.resume(
                                        Result.failure(IllegalStateException("Cloud Anchor resolve failed: $state"))
                                    )
                                }
                            }
                        }
                    }
                        .onSuccess { resolveFuture ->
                            future = resolveFuture
                            continuation.invokeOnCancellation {
                                resolveFuture.cancel()
                            }
                        }
                        .onFailure { error ->
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(error))
                            }
                        }
                }
            }
        } catch (error: TimeoutCancellationException) {
            future?.cancel()
            Result.failure(IllegalStateException("Cloud Anchor resolve timed out for ${cloudAnchorId.take(8)}."))
        } catch (error: Throwable) {
            future?.cancel()
            Result.failure(error)
        }
    }

    private fun assertCloudAnchorModeEnabled(session: Session) {
        assert(session.config.cloudAnchorMode == Config.CloudAnchorMode.ENABLED) {
            "Cloud Anchor mode must be enabled before host/resolve operations."
        }
    }
}
