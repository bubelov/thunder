package api

import android.util.Log
import cln.NodeGrpc
import conf.ConfRepo
import db.AuthCredentials
import db.authCredentials
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
import org.koin.core.annotation.Single
import org.torproject.jni.TorService
import tls.lightningNodeSSLContext
import tor.TorController
import java.net.InetSocketAddress

@Single
class ApiController(
    confRepo: ConfRepo,
    torController: TorController,
) {
    private val _api = MutableStateFlow<NodeGrpc.NodeBlockingStub?>(null)
    val api = _api.asStateFlow()

    init {
        Log.d("api", "Init")

        confRepo.load().map { it.authCredentials() }.distinctUntilChanged().onEach { creds ->
            if (creds.filled()) {
                val sslContext = lightningNodeSSLContext(
                    serverCertificate = creds.serverCertificate,
                    clientCertificate = creds.clientCertificate,
                    clientPrivateKey = creds.clientPrivateKey,
                )

                val channelBuilder = OkHttpChannelBuilder
                    .forTarget(creds.serverUrl)
                    .hostnameVerifier { _, _ -> true }
                    .sslSocketFactory(sslContext.socketFactory)

                if (creds.serverUrl.contains("onion") && !creds.serverUrl.startsWith("http")) {
                    torController.connectionStatus.collectLatest { torStatus ->
                        when (torStatus) {
                            TorService.STATUS_ON -> {
                                val channel = channelBuilder
                                    .proxyDetector {
                                        HttpConnectProxiedSocketAddress
                                            .newBuilder()
                                            .setProxyAddress(InetSocketAddress("localhost", 8118))
                                            .setTargetAddress(it as InetSocketAddress)
                                            .build()
                                    }
                                    .build()

                                _api.update { NodeGrpc.newBlockingStub(channel) }
                            }

                            else -> _api.update { null }
                        }
                    }
                } else {
                    val channel = channelBuilder.build()
                    _api.update { NodeGrpc.newBlockingStub(channel) }
                }
            }
        }.launchIn(GlobalScope)
    }

    private fun AuthCredentials.filled(): Boolean {
        return serverUrl.isNotBlank()
                || serverCertificate.isNotBlank()
                || clientCertificate.isNotBlank()
                || clientPrivateKey.isNotBlank()
    }
}