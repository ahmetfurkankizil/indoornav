package com.Vectura AI.core.di

import com.Vectura AI.core.ar.ArNavigationCoordinator
import com.Vectura AI.core.ar.CorrectionCoordinator
import com.Vectura AI.core.ar.OffRouteDetector
import com.Vectura AI.core.ar.RouteToArrowMapper
import com.Vectura AI.core.config.AppConfig
import com.Vectura AI.core.loading.BuildingPackageLoader
import com.Vectura AI.core.loading.DefaultBuildingRepository
import com.Vectura AI.core.loading.DemoPackageProvider
import com.Vectura AI.core.loading.InMemoryPackageStore
import com.Vectura AI.core.navigation.ArrivalDetector
import com.Vectura AI.core.navigation.DemoMode
import com.Vectura AI.core.navigation.NavigationSessionCoordinator
import com.Vectura AI.core.repository.BuildingRepository
import com.Vectura AI.core.repository.HistoryRepository
import com.Vectura AI.core.repository.InMemoryHistoryRepository
import com.Vectura AI.core.routing.DijkstraRouteEngine
import com.Vectura AI.core.routing.RouteEngine
import com.Vectura AI.core.store.AppStore
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

    // AR — correction & confidence
    single { CorrectionCoordinator() }
    single { OffRouteDetector() }

    // AR
    single { RouteToArrowMapper() }
    single { ArNavigationCoordinator(get(), get(), get(), get(), get(), get()) }

    // Navigation session
    single { ArrivalDetector() }
    single { NavigationSessionCoordinator(get(), get(), get(), get()) }

    // History (in-memory for now; swap to JsonFileHistoryRepository per-platform)
    single<HistoryRepository> { InMemoryHistoryRepository() }
}

