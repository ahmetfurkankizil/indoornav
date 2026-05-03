package com.Vectura AI.android.di

import com.Vectura AI.android.ar.AndroidArNavigationViewModel
import com.Vectura AI.android.ar.AndroidHapticManager
import com.Vectura AI.android.ar.ArBridge
import com.Vectura AI.android.ar.ArMarkerDetector
import com.Vectura AI.android.ar.ArRouteRenderer
import com.Vectura AI.android.data.AndroidReviewedPackageLoader
import com.Vectura AI.android.navigation.ArCameraFlowViewModel
import com.Vectura AI.android.navigation.AndroidNavigationFlowModel
import com.Vectura AI.android.Vectura AIConfig
import com.Vectura AI.data.remote.KtorBuildingDataSource
import com.Vectura AI.data.remote.RemoteBuildingDataSource
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
        KtorBuildingDataSource(Vectura AIConfig.API_BASE_URL)
    }

    factory { ArMarkerDetector() }
    factory { ArRouteRenderer() }

    viewModel { AndroidNavigationFlowModel(get(), get()) }
    viewModel { ArCameraFlowViewModel(get(), get()) }
    viewModel { AndroidArNavigationViewModel(get(), get(), get()) }

    // TODO: Provide Android-specific SqlDelight driver
    // TODO: Provide Android-specific Ktor engine (OkHttp)
}
