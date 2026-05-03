package com.Vectura AI.feature.routing.di

import com.Vectura AI.feature.routing.RouteNavigationUseCase
import org.koin.dsl.module

val featureRoutingModule = module {
    factory { RouteNavigationUseCase(
        buildingRepository = get(),
        routeEngine = get(),
        appStore = get(),
    ) }
}
