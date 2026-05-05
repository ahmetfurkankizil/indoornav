package com.VecturAI.data.local.di

import com.VecturAI.data.local.LocalCacheDataSource
import com.VecturAI.data.local.SqlDelightCacheDataSource
import org.koin.dsl.module

val dataLocalModule = module {
    single<LocalCacheDataSource> { SqlDelightCacheDataSource() }
}
