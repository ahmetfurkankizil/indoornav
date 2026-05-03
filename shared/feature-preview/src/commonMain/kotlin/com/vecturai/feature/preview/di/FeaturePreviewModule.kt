package com.Vectura AI.feature.preview.di

import com.Vectura AI.feature.preview.RoutePreviewUseCase
import org.koin.dsl.module

val featurePreviewModule = module {
    factory { RoutePreviewUseCase(
        buildingRepository = get(),
        routeEngine = get(),
    ) }
}
