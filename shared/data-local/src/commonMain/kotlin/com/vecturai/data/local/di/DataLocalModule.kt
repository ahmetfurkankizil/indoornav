package com.Vectura AI.data.local.di

import com.Vectura AI.data.local.LocalCacheDataSource
import com.Vectura AI.data.local.SqlDelightCacheDataSource
import org.koin.dsl.module

val dataLocalModule = module {
    single<LocalCacheDataSource> { SqlDelightCacheDataSource() }
}
