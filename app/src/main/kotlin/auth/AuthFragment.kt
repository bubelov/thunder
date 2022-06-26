package auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.FragmentAuthBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import conf.ConfRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.android.ext.android.get

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val confRepo by lazy { get<ConfRepo>() }

    private val blitzScanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            val json = JSONObject(result.contents)

            viewLifecycleOwner.lifecycleScope.launch {
                val confRepo = get<ConfRepo>()

                confRepo.save {
                    it.copy(
                        serverUrl = json.getString("url"),
                        serverCertificate = json.getString("server_pem"),
                        clientCertificate = json.getString("client_pem"),
                        clientPrivateKey = json.getString("client_key_pem"),
                    )
                }

                findNavController().navigate(R.id.authFragment_toPaymentsFragment)
            }
        }
    }

    private val serverHostnameScanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    val host = result.contents.replace("\n", "")
                    val port = binding.serverPort.text.toString()
                    confRepo.save { it.copy(serverUrl = "$host:$port") }
                }
            }
        }
    }

    private val serverCertificateScanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    confRepo.save { it.copy(serverCertificate = result.contents.replace("\r\n", "\n")) }
                }
            }
        }
    }

    private val clientCertificateScanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    confRepo.save { it.copy(clientCertificate = result.contents.replace("\r\n", "\n")) }
                }
            }
        }
    }

    private val clientPrivateKeyScanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    confRepo.save { it.copy(clientPrivateKey = result.contents.replace("\r\n", "\n")) }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.scanBlitzCredentials.setOnClickListener {
            blitzScanner.launch(ScanOptions())
        }

        binding.scanServerHostname.setOnClickListener {
            serverHostnameScanner.launch(ScanOptions())
        }

        binding.scanServerCertificate.setOnClickListener {
            serverCertificateScanner.launch(ScanOptions())
        }

        binding.scanClientCertificate.setOnClickListener {
            clientCertificateScanner.launch(ScanOptions())
        }

        binding.scanClientKey.setOnClickListener {
            clientPrivateKeyScanner.launch(ScanOptions())
        }

        binding.connect.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                val confRepo = get<ConfRepo>()
                val conf = confRepo.load().first()
                val serverUrl = conf.serverUrl
                val host = serverUrl.split(":").first()
                confRepo.save { it.copy(serverUrl = "$host:${binding.serverPort.text}") }
                findNavController().navigate(R.id.authFragment_toPaymentsFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}