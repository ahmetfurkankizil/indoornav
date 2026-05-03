package com.Vectura AI.feature.search.di

import com.Vectura AI.feature.search.SearchUseCase
import org.koin.dsl.module

val featureSearchModule = module {
    factory { SearchUseCase(buildingRepository = get()) }
}
