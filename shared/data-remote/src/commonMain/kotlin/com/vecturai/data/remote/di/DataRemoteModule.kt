package com.Vectura AI.data.remote.di

import com.Vectura AI.data.remote.KtorBuildingDataSource
import com.Vectura AI.data.remote.RemoteBuildingDataSource
import org.koin.dsl.module

val dataRemoteModule = module {
    single<RemoteBuildingDataSource> { (baseUrl: String) -> KtorBuildingDataSource(baseUrl) }
}
