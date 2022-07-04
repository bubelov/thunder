package tor

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import conf.ConfRepo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import org.torproject.jni.TorService

@Single
class TorController(
    private val app: Application,
    private val confRepo: ConfRepo,
) {

    private val _connectionStatus = MutableStateFlow("")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val serviceConnection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {}
            override fun onServiceDisconnected(name: ComponentName) {}
        }
    }

    private var bound = false

    init {
        app.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val status = intent.getStringExtra(TorService.EXTRA_STATUS)!!
                    Log.d("tor", "New status: $status")
                    _connectionStatus.value = status
                }
            },
            IntentFilter(TorService.ACTION_STATUS),
        )

        confRepo.load()
            .map { it.serverUrl }
            .distinctUntilChanged()
            .onEach { onServerUrlChanged(it) }
            .launchIn(GlobalScope)
    }

    private fun onServerUrlChanged(url: String) {
        if (url.contains(".onion:")) {
            if (!bound) {
                app.bindService(
                    Intent(app, TorService::class.java),
                    serviceConnection,
                    Application.BIND_AUTO_CREATE,
                )

                bound = true
            }
        } else {
            if (bound) {
                app.unbindService(serviceConnection)
                bound = false
            }
        }
    }
}