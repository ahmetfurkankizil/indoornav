package com.vecturai.android.di

import com.vecturai.android.ar.AndroidArNavigationViewModel
import com.vecturai.android.ar.AndroidHapticManager
import com.vecturai.android.ar.ArBridge
import com.vecturai.android.ar.ArMarkerDetector
import com.vecturai.android.ar.ArRouteRenderer
import com.vecturai.android.data.AndroidReviewedPackageLoader
import com.vecturai.android.navigation.ArCameraFlowViewModel
import com.vecturai.android.navigation.AndroidNavigationFlowModel
import com.vecturai.android.VecturaiConfig
import com.vecturai.data.remote.KtorBuildingDataSource
import com.vecturai.data.remote.RemoteBuildingDataSource
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
        KtorBuildingDataSource(VecturaiConfig.API_BASE_URL)
    }

    factory { ArMarkerDetector() }
    factory { ArRouteRenderer() }

    viewModel { AndroidNavigationFlowModel(get()) }
    viewModel { ArCameraFlowViewModel(get(), get()) }
    viewModel { AndroidArNavigationViewModel(get(), get(), get()) }

    // TODO: Provide Android-specific SqlDelight driver
    // TODO: Provide Android-specific Ktor engine (OkHttp)
}
