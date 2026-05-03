package com.Vectura AI.android

import android.app.Application
import com.Vectura AI.android.di.androidModule
import com.Vectura AI.core.di.coreModule
import com.Vectura AI.data.local.di.dataLocalModule
import com.Vectura AI.data.remote.di.dataRemoteModule
import com.Vectura AI.feature.history.di.featureHistoryModule
import com.Vectura AI.feature.preview.di.featurePreviewModule
import com.Vectura AI.feature.routing.di.featureRoutingModule
import com.Vectura AI.feature.search.di.featureSearchModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Vectura AIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Vectura AIApp)
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
