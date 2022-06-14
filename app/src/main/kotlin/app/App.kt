package app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import db.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule
import org.torproject.jni.BuildConfig
import org.torproject.jni.TorService

class App : Application() {

    private val _torConnectionStatus = MutableStateFlow("")
    val torConnectionStatus = _torConnectionStatus.asStateFlow()

    private val torBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra(TorService.EXTRA_STATUS)!!
            _torConnectionStatus.value = status
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)
            androidContext(applicationContext)
            defaultModule()
            modules(module { single { database(applicationContext) } })
        }

        registerReceiver(
            torBroadcastReceiver,
            IntentFilter(TorService.ACTION_STATUS),
        )

        bindService(
            Intent(this, TorService::class.java),
            SilentServiceConnection(),
            BIND_AUTO_CREATE,
        )
    }

    private class SilentServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {}
    }
}