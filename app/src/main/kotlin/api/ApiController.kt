package api

import android.util.Log
import cln.NodeGrpc
import conf.ConfRepo
import db.authCredentials
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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

        combine(
            torController.connectionStatus,
            confRepo.load().map { it.authCredentials() }.distinctUntilChanged(),
        ) { torStatus, creds ->
            Log.d("api", "Tor status changed to $torStatus")

            when (torStatus) {
                TorService.STATUS_ON -> {
                    if (
                        creds.serverUrl.isBlank()
                        || creds.serverCertificate.isBlank()
                        || creds.clientCertificate.isBlank()
                        || creds.clientPrivateKey.isBlank()
                    ) {
                        _api.update { null }
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

                        _api.update { NodeGrpc.newBlockingStub(channel) }
                    }
                }

                else -> _api.update { null }
            }
        }.launchIn(GlobalScope)
    }
}