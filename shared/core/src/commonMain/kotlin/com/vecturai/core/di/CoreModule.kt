package com.vecturai.core.di

import com.vecturai.core.loading.BuildingPackageLoader
import com.vecturai.core.loading.DefaultBuildingRepository
import com.vecturai.core.loading.InMemoryPackageStore
import com.vecturai.core.repository.BuildingRepository
import com.vecturai.core.routing.DijkstraRouteEngine
import com.vecturai.core.routing.RouteEngine
import com.vecturai.core.store.AppStore
import org.koin.dsl.module

/**
 * Core Koin DI module providing shared singletons.
 */
val coreModule = module {
    single { AppStore() }
    single<RouteEngine> { DijkstraRouteEngine() }
    single { InMemoryPackageStore() }
    single { BuildingPackageLoader() }
    single<BuildingRepository> { DefaultBuildingRepository(get()) }
}
