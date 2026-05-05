package com.VecturAI.android

import android.app.Application
import com.VecturAI.android.di.androidModule
import com.VecturAI.core.di.coreModule
import com.VecturAI.data.local.di.dataLocalModule
import com.VecturAI.data.remote.di.dataRemoteModule
import com.VecturAI.feature.history.di.featureHistoryModule
import com.VecturAI.feature.preview.di.featurePreviewModule
import com.VecturAI.feature.routing.di.featureRoutingModule
import com.VecturAI.feature.search.di.featureSearchModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VecturAIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VecturAIApp)
            modules(
                coreModule,
                dataLocalModule,
                featureSearchModule,
                featureRoutingModule,
                featureHistoryModule,
                featurePreviewModule,
                androidModule,
            )
        }
    }
}
