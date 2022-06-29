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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import org.torproject.jni.TorService

@Single
class TorController(
    app: Application,
) {

    private val _connectionStatus = MutableStateFlow("")
    val connectionStatus = _connectionStatus.asStateFlow()

    init {
        Log.d("tor", "Init")

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

        app.bindService(
            Intent(app, TorService::class.java),
            SilentServiceConnection(),
            Application.BIND_AUTO_CREATE,
        )
    }

    private class SilentServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {}
    }
}