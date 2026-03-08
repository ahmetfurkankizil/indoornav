package com.vecturai.core.di

import com.vecturai.core.ar.ArNavigationCoordinator
import com.vecturai.core.ar.RouteToArrowMapper
import com.vecturai.core.loading.BuildingPackageLoader
import com.vecturai.core.loading.DefaultBuildingRepository
import com.vecturai.core.loading.InMemoryPackageStore
import com.vecturai.core.navigation.ArrivalDetector
import com.vecturai.core.navigation.DemoMode
import com.vecturai.core.navigation.NavigationSessionCoordinator
import com.vecturai.core.repository.BuildingRepository
import com.vecturai.core.repository.HistoryRepository
import com.vecturai.core.repository.InMemoryHistoryRepository
import com.vecturai.core.routing.DijkstraRouteEngine
import com.vecturai.core.routing.RouteEngine
import com.vecturai.core.store.AppStore
import com.vecturai.feature.history.HistoryUseCase
import com.vecturai.feature.preview.RoutePreviewUseCase
import org.koin.dsl.module

/**
 * Core Koin DI module providing shared singletons.
 */
val coreModule = module {
    // State
    single { AppStore() }

    // Routing
    single<RouteEngine> { DijkstraRouteEngine() }
    single { RoutePreviewUseCase(get(), get()) }

    // Package loading
    single { InMemoryPackageStore() }
    single { BuildingPackageLoader() }
    single<BuildingRepository> { DefaultBuildingRepository(get()) }

    // AR
    single { RouteToArrowMapper() }
    single { ArNavigationCoordinator(get(), get(), get(), get()) }

    // Navigation session
    single { ArrivalDetector() }
    single { NavigationSessionCoordinator(get(), get(), get(), get()) }
    single { DemoMode() }

    // History
    single<HistoryRepository> { InMemoryHistoryRepository() }
    single { HistoryUseCase(get()) }
}
