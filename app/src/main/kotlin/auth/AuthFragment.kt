package auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.FragmentAuthBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import conf.ConfRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthFragment : Fragment() {

    private val model: AuthModel by viewModel()

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val serverUrlScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.let {
            binding.serverUrl.setText(it)
            model.saveServerUrl(it)
        }
    }

    private val serverCertScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.trim('\n')?.let {
            binding.serverCert.setText(it)
            model.saveServerCert(it)
        }
    }

    private val clientCertScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.trim('\n')?.let {
            binding.clientCert.setText(it)
            model.saveClientCert(it)
        }
    }

    private val clientKeyScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.trim('\n')?.let {
            binding.clientKey.setText(it)
            model.saveClientKey(it)
        }
    }

    private val blitzScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.let { qr ->
            val json = JSONObject(qr)

            val serverUrl = json.getString("url")
            val serverCert = json.getString("server_pem").trim('\n')
            val clientCert = json.getString("client_pem").trim('\n')
            val clientKey = json.getString("client_key_pem").trim('\n')

            binding.serverUrl.setText(serverUrl)
            binding.serverCert.setText(serverCert)
            binding.clientCert.setText(clientCert)
            binding.clientKey.setText(clientKey)

            viewLifecycleOwner.lifecycleScope.launch {
                model.saveConf {
                    it.copy(
                        serverUrl = serverUrl,
                        serverCertPem = serverCert,
                        clientCertPem = clientCert,
                        clientKeyPem = clientKey,
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val conf = runBlocking { get<ConfRepo>().load().first() }

        return if (conf.authCompleted) {
            findNavController().navigate(R.id.authFragment_toPaymentsFragment)
            null
        } else {
            _binding = FragmentAuthBinding.inflate(inflater, container, false)
            binding.serverUrl.setText(conf.serverUrl)
            binding.serverCert.setText(conf.serverCertPem)
            binding.clientCert.setText(conf.clientCertPem)
            binding.clientKey.setText(conf.clientKeyPem)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.serverUrlLayout.setEndIconOnClickListener { serverUrlScanner.launch(ScanOptions()) }
        binding.serverCertLayout.setEndIconOnClickListener { serverCertScanner.launch(ScanOptions()) }
        binding.clientCertLayout.setEndIconOnClickListener { clientCertScanner.launch(ScanOptions()) }
        binding.clientKeyLayout.setEndIconOnClickListener { clientKeyScanner.launch(ScanOptions()) }

        binding.scanBlitzQr.setOnClickListener { blitzScanner.launch(ScanOptions()) }

        binding.connect.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                model.saveConf {
                    it.copy(
                        serverUrl = binding.serverUrl.text.toString(),
                        serverCertPem = binding.serverCert.text.toString(),
                        clientCertPem = binding.clientCert.text.toString(),
                        clientKeyPem = binding.clientKey.text.toString(),
                    )
                }

                runCatching {
                    model.testConnection()
                }.onSuccess {
                    model.saveConf { it.copy(authCompleted = true) }
                    findNavController().navigate(R.id.authFragment_toPaymentsFragment)
                }.onFailure {
                    Toast.makeText(requireContext(), "Connection test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}