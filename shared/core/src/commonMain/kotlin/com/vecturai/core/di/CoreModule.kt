package com.vecturai.core.di

import com.vecturai.core.ar.ArNavigationCoordinator
import com.vecturai.core.ar.RouteToArrowMapper
import com.vecturai.core.config.AppConfig
import com.vecturai.core.loading.BuildingPackageLoader
import com.vecturai.core.loading.DefaultBuildingRepository
import com.vecturai.core.loading.DemoPackageProvider
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
import org.koin.dsl.module

/**
 * Core Koin DI module providing shared singletons.
 *
 * Wires all shared logic: routing, AR, navigation session,
 * history, configuration, and demo support.
 */
val coreModule = module {
    // Config
    single { AppConfig.current }

    // State
    single { AppStore() }

    // Routing
    single<RouteEngine> { DijkstraRouteEngine() }

    // Package loading
    single { InMemoryPackageStore() }
    single { BuildingPackageLoader() }
    single<BuildingRepository> { DefaultBuildingRepository(get()) }

    // Demo
    single { DemoPackageProvider }
    single { DemoMode() }

    // AR
    single { RouteToArrowMapper() }
    single { ArNavigationCoordinator(get(), get(), get(), get()) }

    // Navigation session
    single { ArrivalDetector() }
    single { NavigationSessionCoordinator(get(), get(), get(), get()) }

    // History (in-memory for now; swap to JsonFileHistoryRepository per-platform)
    single<HistoryRepository> { InMemoryHistoryRepository() }
}
