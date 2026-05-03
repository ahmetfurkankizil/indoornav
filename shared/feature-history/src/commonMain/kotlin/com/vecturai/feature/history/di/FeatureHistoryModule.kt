package com.Vectura AI.feature.history.di

import com.Vectura AI.feature.history.HistoryUseCase
import org.koin.dsl.module

val featureHistoryModule = module {
    factory { HistoryUseCase(historyRepository = get()) }
}
