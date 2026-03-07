package com.vecturai.feature.search.di

import com.vecturai.feature.search.SearchUseCase
import org.koin.dsl.module

val featureSearchModule = module {
    factory { SearchUseCase(buildingRepository = get()) }
}
