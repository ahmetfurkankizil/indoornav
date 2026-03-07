package com.vecturai.core.di

import com.vecturai.core.routing.DijkstraRouteEngine
import com.vecturai.core.routing.RouteEngine
import com.vecturai.core.store.AppStore
import org.koin.dsl.module

/**
 * Koin module for shared core dependencies.
 *
 * Provides singleton instances of:
 * - [AppStore] — central state holder
 * - [RouteEngine] — routing algorithm (Dijkstra for MVP)
 */
val coreModule = module {
    single { AppStore() }
    single<RouteEngine> { DijkstraRouteEngine() }
}
