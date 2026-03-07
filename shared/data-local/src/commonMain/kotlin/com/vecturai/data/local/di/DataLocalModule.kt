package com.vecturai.data.local.di

import com.vecturai.data.local.LocalCacheDataSource
import com.vecturai.data.local.SqlDelightCacheDataSource
import org.koin.dsl.module

val dataLocalModule = module {
    single<LocalCacheDataSource> { SqlDelightCacheDataSource() }
}
