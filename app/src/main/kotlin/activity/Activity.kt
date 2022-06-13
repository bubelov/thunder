package activity

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cln.NodeGrpc
import cln.NodeOuterClass
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.ActivityBinding
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.torproject.jni.TorService
import tls.lightningNodeSSLContext
import java.net.InetSocketAddress
import java.text.NumberFormat

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private val torBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra(TorService.EXTRA_STATUS)

            when (status) {
                TorService.STATUS_STARTING -> {
                    binding.progressIndicator.visibility = View.VISIBLE
                    binding.progressMessage.setText(R.string.connecting_to_tor_network)
                }

                TorService.STATUS_ON -> {
                    binding.progressMessage.setText(R.string.loading_data)
                }
            }

            if (status == TorService.STATUS_ON) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Default) {
                        val sslContext = lightningNodeSSLContext(
                            serverCertificate = getString(R.string.server_certificate),
                            clientCertificate = getString(R.string.client_certificate),
                            clientPrivateKey = getString(R.string.client_private_key),
                        )

                        val channel = OkHttpChannelBuilder
                            .forTarget(getString(R.string.server_url))
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

                        val node = NodeGrpc.newBlockingStub(channel)
                        val listFundsResponse = node.listFunds(NodeOuterClass.ListfundsRequest.getDefaultInstance())

                        val totalFundsMsat = listFundsResponse.channelsList.sumOf { it.ourAmountMsat.msat }

                        withContext(Dispatchers.Main) {
                            binding.progressIndicator.visibility = View.GONE
                            binding.progressMessage.visibility = View.GONE
                            binding.satsInChannels.text = getString(
                                R.string.s_sats,
                                NumberFormat.getNumberInstance().format(totalFundsMsat / 1000)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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