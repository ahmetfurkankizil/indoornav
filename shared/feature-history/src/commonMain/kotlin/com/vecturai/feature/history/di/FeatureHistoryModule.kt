package com.vecturai.feature.history.di

import com.vecturai.feature.history.HistoryUseCase
import org.koin.dsl.module

val featureHistoryModule = module {
    factory { HistoryUseCase(historyRepository = get()) }
}
