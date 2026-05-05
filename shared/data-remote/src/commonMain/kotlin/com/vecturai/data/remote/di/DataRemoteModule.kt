package com.VecturAI.data.remote.di

import com.VecturAI.data.remote.KtorBuildingDataSource
import com.VecturAI.data.remote.RemoteBuildingDataSource
import org.koin.dsl.module

val dataRemoteModule = module {
    single<RemoteBuildingDataSource> { (baseUrl: String) -> KtorBuildingDataSource(baseUrl) }
}
