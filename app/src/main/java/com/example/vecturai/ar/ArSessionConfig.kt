package com.example.vecturai.ar

import com.google.ar.core.Config
import com.google.ar.core.Session

object ArSessionConfig {
    fun configureIndoorCloudSession(session: Session, config: Config) {
        config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
        config.focusMode = Config.FocusMode.AUTO
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        }
    }
}
