package com.vecturai.feature.routing.di

import com.vecturai.feature.routing.RouteNavigationUseCase
import org.koin.dsl.module

val featureRoutingModule = module {
    factory { RouteNavigationUseCase(
        buildingRepository = get(),
        routeEngine = get(),
        appStore = get(),
    ) }
}
