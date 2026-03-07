package com.vecturai.android

import android.app.Application
import com.vecturai.android.di.androidModule
import com.vecturai.core.di.coreModule
import com.vecturai.data.local.di.dataLocalModule
import com.vecturai.data.remote.di.dataRemoteModule
import com.vecturai.feature.history.di.featureHistoryModule
import com.vecturai.feature.preview.di.featurePreviewModule
import com.vecturai.feature.routing.di.featureRoutingModule
import com.vecturai.feature.search.di.featureSearchModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VecturaiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VecturaiApp)
            modules(
                coreModule,
                dataLocalModule,
                dataRemoteModule,
                featureSearchModule,
                featureRoutingModule,
                featureHistoryModule,
                featurePreviewModule,
                androidModule,
            )
        }
    }
}
