package app

import android.app.Application
import db.database
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule
import org.torproject.jni.BuildConfig
import sync.Sync

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)
            androidContext(applicationContext)
            defaultModule()
            modules(module { single { database(applicationContext) } })
        }

        val sync = get<Sync>()
        GlobalScope.launch { sync.sync() }
    }
}