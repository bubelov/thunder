package api

import cln.NodeGrpc
import db.AuthCredentials
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.delay
import org.torproject.jni.TorService
import tls.lightningNodeSSLContext
import tor.TorController
import java.net.InetSocketAddress

class ApiBuilder {

    suspend fun build(creds: AuthCredentials, torController: TorController): NodeGrpc.NodeBlockingStub {
        val sslContext = lightningNodeSSLContext(
            serverCertPem = creds.serverCertPem,
            clientCertPem = creds.clientCertPem,
            clientKeyPem = creds.clientKeyPem,
        )

        val channelBuilder = OkHttpChannelBuilder
            .forTarget(creds.serverUrl)
            .hostnameVerifier { _, _ -> true }
            .sslSocketFactory(sslContext.socketFactory)

        return if (creds.serverUrl.contains("onion") && !creds.serverUrl.startsWith("http")) {
            while (torController.connectionStatus.value != TorService.STATUS_ON) {
                delay(100)
            }

            val channel = channelBuilder
                .proxyDetector {
                    HttpConnectProxiedSocketAddress
                        .newBuilder()
                        .setProxyAddress(InetSocketAddress("localhost", 8118))
                        .setTargetAddress(it as InetSocketAddress)
                        .build()
                }
                .build()

            NodeGrpc.newBlockingStub(channel)
        } else {
            val channel = channelBuilder.build()
            NodeGrpc.newBlockingStub(channel)
        }
    }
}