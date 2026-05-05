package com.VecturAI.core.di

import com.VecturAI.core.ar.ArNavigationCoordinator
import com.VecturAI.core.ar.CorrectionCoordinator
import com.VecturAI.core.ar.OffRouteDetector
import com.VecturAI.core.ar.RouteToArrowMapper
import com.VecturAI.core.config.AppConfig
import com.VecturAI.core.loading.BuildingPackageLoader
import com.VecturAI.core.loading.DefaultBuildingRepository
import com.VecturAI.core.loading.DemoPackageProvider
import com.VecturAI.core.loading.InMemoryPackageStore
import com.VecturAI.core.navigation.ArrivalDetector
import com.VecturAI.core.navigation.DemoMode
import com.VecturAI.core.navigation.NavigationSessionCoordinator
import com.VecturAI.core.repository.BuildingRepository
import com.VecturAI.core.repository.HistoryRepository
import com.VecturAI.core.repository.InMemoryHistoryRepository
import com.VecturAI.core.routing.DijkstraRouteEngine
import com.VecturAI.core.routing.RouteEngine
import com.VecturAI.core.store.AppStore
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

