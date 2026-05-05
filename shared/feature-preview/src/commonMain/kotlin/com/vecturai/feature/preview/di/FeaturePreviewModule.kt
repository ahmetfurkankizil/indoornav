package com.VecturAI.feature.preview.di

import com.VecturAI.feature.preview.RoutePreviewUseCase
import org.koin.dsl.module

val featurePreviewModule = module {
    factory { RoutePreviewUseCase(
        buildingRepository = get(),
        routeEngine = get(),
    ) }
}
