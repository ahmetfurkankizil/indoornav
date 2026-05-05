package com.VecturAI.feature.routing.di

import com.VecturAI.feature.routing.RouteNavigationUseCase
import org.koin.dsl.module

val featureRoutingModule = module {
    factory { RouteNavigationUseCase(
        buildingRepository = get(),
        routeEngine = get(),
        appStore = get(),
    ) }
}
