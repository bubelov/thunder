package app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import cln.NodeGrpc
import conf.ConfRepo
import db.authCredentials
import db.database
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule
import org.torproject.jni.BuildConfig
import org.torproject.jni.TorService
import tls.lightningNodeSSLContext
import java.net.InetSocketAddress

class App : Application() {

    private val _torConnectionStatus = MutableStateFlow("")
    val torConnectionStatus = _torConnectionStatus.asStateFlow()

    private val _node = MutableStateFlow<NodeGrpc.NodeBlockingStub?>(null)
    val node = _node.asStateFlow()

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

        torConnectionStatus.onEach { status ->
            if (status == TorService.STATUS_ON) {
                get<ConfRepo>()
                    .load()
                    .map { it.authCredentials() }
                    .distinctUntilChanged()
                    .collectLatest { creds ->
                        if (
                            creds.serverUrl.isBlank()
                            || creds.serverCertificate.isBlank()
                            || creds.clientCertificate.isBlank()
                            || creds.clientPrivateKey.isBlank()
                        ) {
                            _node.update { null }
                        } else {
                            val sslContext = lightningNodeSSLContext(
                                serverCertificate = creds.serverCertificate,
                                clientCertificate = creds.clientCertificate,
                                clientPrivateKey = creds.clientPrivateKey,
                            )

                            val channel = OkHttpChannelBuilder
                                .forTarget(creds.serverUrl)
                                .hostnameVerifier { _, _ -> true }
                                .sslSocketFactory(sslContext.socketFactory)
                                .proxyDetector {
                                    HttpConnectProxiedSocketAddress
                                        .newBuilder()
                                        .setProxyAddress(InetSocketAddress("localhost", 8118))
                                        .setTargetAddress(it as InetSocketAddress)
                                        .build()
                                }
                                .build()

                            _node.update { NodeGrpc.newBlockingStub(channel) }
                        }
                    }
            } else {
                _node.update { null }
            }
        }.launchIn(GlobalScope)
    }

    private class SilentServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {}
    }
}