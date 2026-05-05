package com.VecturAI.android.di

import com.VecturAI.android.ar.AndroidArNavigationViewModel
import com.VecturAI.android.ar.AndroidHapticManager
import com.VecturAI.android.ar.ArBridge
import com.VecturAI.android.ar.ArMarkerDetector
import com.VecturAI.android.ar.ArRouteRenderer
import com.VecturAI.android.data.AndroidReviewedPackageLoader
import com.VecturAI.android.navigation.ArCameraFlowViewModel
import com.VecturAI.android.navigation.AndroidNavigationFlowModel
import com.VecturAI.android.VecturAIConfig
import com.VecturAI.data.remote.KtorBuildingDataSource
import com.VecturAI.data.remote.RemoteBuildingDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 * Provides platform-specific implementations and Android-only dependencies.
 */
val androidModule = module {
    single { ArBridge() }
    single { AndroidReviewedPackageLoader(androidContext()) }
    single { AndroidHapticManager(androidContext()) }

    // Use our PC's IP for the remote data source
    single<RemoteBuildingDataSource> { 
        KtorBuildingDataSource(VecturAIConfig.API_BASE_URL)
    }

    factory { ArMarkerDetector() }
    factory { ArRouteRenderer() }

    viewModel { AndroidNavigationFlowModel(get(), get()) }
    viewModel { ArCameraFlowViewModel(get(), get()) }
    viewModel { AndroidArNavigationViewModel(get(), get(), get()) }

    // TODO: Provide Android-specific SqlDelight driver
    // TODO: Provide Android-specific Ktor engine (OkHttp)
}
