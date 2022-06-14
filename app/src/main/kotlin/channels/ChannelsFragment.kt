package channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.App
import cln.NodeGrpc
import cln.NodeOuterClass
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.FragmentChannelsBinding
import conf.ConfRepo
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.torproject.jni.TorService
import tls.lightningNodeSSLContext
import java.net.InetSocketAddress

class ChannelsFragment : Fragment() {

    private var _binding: FragmentChannelsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ChannelsAdapter()
        binding.items.adapter = adapter
        binding.items.layoutManager = LinearLayoutManager(requireContext())


        val listItemDecoration = CardListAdapterDecoration(
            resources.getDimensionPixelSize(R.dimen.card_padding)
        )

        binding.items.addItemDecoration(listItemDecoration)

        val app = requireActivity().application as App

        app.torConnectionStatus.onEach { status ->
            when (status) {
                TorService.STATUS_STARTING -> {
                    binding.progressIndicator.visibility = View.VISIBLE
                    binding.progressMessage.setText(R.string.connecting_to_tor_network)
                }

                TorService.STATUS_ON -> {
                    binding.progressMessage.setText(R.string.loading_data)

                    lifecycleScope.launch {
                        withContext(Dispatchers.Default) {
                            val conf = get<ConfRepo>().load().first()

                            val sslContext = lightningNodeSSLContext(
                                serverCertificate = conf.serverCertificate,
                                clientCertificate = conf.clientCertificate,
                                clientPrivateKey = conf.clientPrivateKey,
                            )

                            val channel = OkHttpChannelBuilder
                                .forTarget(conf.serverUrl)
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

                            withContext(Dispatchers.Main) {
                                binding.progressIndicator.visibility = View.GONE
                                binding.progressMessage.visibility = View.GONE
                                adapter.submitList(listFundsResponse.channelsList)
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