package com.vecturai.android.di

import com.vecturai.android.ar.ArBridge
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 * Provides platform-specific implementations and Android-only dependencies.
 */
val androidModule = module {
    single { ArBridge() }

    // TODO: Provide Android-specific SqlDelight driver
    // TODO: Provide Android-specific Ktor engine (OkHttp)
}
