package com.vecturai.data.remote.di

import com.vecturai.data.remote.KtorBuildingDataSource
import com.vecturai.data.remote.RemoteBuildingDataSource
import org.koin.dsl.module

val dataRemoteModule = module {
    single<RemoteBuildingDataSource> { (baseUrl: String) -> KtorBuildingDataSource(baseUrl) }
}
