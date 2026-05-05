package com.VecturAI.feature.history.di

import com.VecturAI.feature.history.HistoryUseCase
import org.koin.dsl.module

val featureHistoryModule = module {
    factory { HistoryUseCase(historyRepository = get()) }
}
