package payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import app.App
import cln.NodeGrpc
import cln.NodeOuterClass
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.FragmentPaymentsBinding
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.torproject.jni.TorService
import tls.lightningNodeSSLContext
import java.net.InetSocketAddress
import java.text.NumberFormat

class PaymentsFragment : Fragment() {

    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireActivity().application as App

        app.torConnectionStatus.onEach { status ->
            when (status) {
                TorService.STATUS_STARTING -> {
                    binding.progressIndicator.isVisible = true
                    binding.progressMessage.isVisible = true
                    binding.progressMessage.setText(R.string.connecting_to_tor_network)
                    binding.satsInChannels.isVisible = false
                    binding.send.isVisible = false
                    binding.receive.isVisible = false
                }

                TorService.STATUS_ON -> {
                    binding.progressIndicator.isVisible = true
                    binding.progressMessage.isVisible = true
                    binding.progressMessage.setText(R.string.loading_data)
                    binding.satsInChannels.isVisible = false
                    binding.send.isVisible = false
                    binding.receive.isVisible = false

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
                                binding.progressIndicator.isVisible = false
                                binding.progressMessage.isVisible = false
                                binding.progressMessage.setText(R.string.loading_data)
                                binding.satsInChannels.isVisible = true
                                binding.satsInChannels.text = getString(
                                    R.string.s_sats,
                                    NumberFormat.getNumberInstance().format(totalFundsMsat / 1000)
                                )
                                binding.send.isVisible = true
                                binding.receive.isVisible = true
                            }
                        }
                    }
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}