package com.VecturAI.feature.search.di

import com.VecturAI.feature.search.SearchUseCase
import org.koin.dsl.module

val featureSearchModule = module {
    factory { SearchUseCase(buildingRepository = get()) }
}
