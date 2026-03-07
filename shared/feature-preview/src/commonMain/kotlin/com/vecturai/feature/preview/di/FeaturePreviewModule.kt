package com.vecturai.feature.preview.di

import com.vecturai.feature.preview.RoutePreviewUseCase
import org.koin.dsl.module

val featurePreviewModule = module {
    factory { RoutePreviewUseCase(
        buildingRepository = get(),
        routeEngine = get(),
    ) }
}
