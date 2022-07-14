package auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
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
            binding.serverCertPem.setText(it)
            model.saveServerCert(it)
        }
    }

    private val clientCertScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.trim('\n')?.let {
            binding.clientCertPem.setText(it)
            model.saveClientCert(it)
        }
    }

    private val clientKeyScanner = registerForActivityResult(ScanContract()) { res ->
        res.contents?.trim('\n')?.let {
            binding.clientKeyPem.setText(it)
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
            binding.serverCertPem.setText(serverCert)
            binding.clientCertPem.setText(clientCert)
            binding.clientKeyPem.setText(clientKey)

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
            binding.serverCertPem.setText(conf.serverCertPem)
            binding.clientCertPem.setText(conf.clientCertPem)
            binding.clientKeyPem.setText(conf.clientKeyPem)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.serverUrlLayout.setEndIconOnClickListener { serverUrlScanner.launch(ScanOptions()) }

        binding.serverUrl.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.serverUrlLayout.error = null
            }
        }

        binding.serverCertPemLayout.setEndIconOnClickListener { serverCertScanner.launch(ScanOptions()) }

        binding.serverCertPem.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.serverCertPemLayout.error = null
            }
        }

        binding.clientCertPemLayout.setEndIconOnClickListener { clientCertScanner.launch(ScanOptions()) }

        binding.clientCertPem.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.clientCertPemLayout.error = null
            }
        }

        binding.clientKeyPemLayout.setEndIconOnClickListener { clientKeyScanner.launch(ScanOptions()) }

        binding.clientKeyPem.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.clientKeyPemLayout.error = null
            }
        }

        binding.scanBlitzQr.setOnClickListener { blitzScanner.launch(ScanOptions()) }

        binding.connect.setOnClickListener {
            var hasValidationErrors = false

            if (binding.serverUrl.length() == 0) {
                binding.serverUrlLayout.error = getString(R.string.url_is_missing)
                hasValidationErrors = true
            }

            if (binding.serverCertPem.length() == 0) {
                binding.serverCertPemLayout.error = getString(R.string.certificate_is_missing)
                hasValidationErrors = true
            }

            if (binding.clientCertPem.length() == 0) {
                binding.clientCertPemLayout.error = getString(R.string.certificate_is_missing)
                hasValidationErrors = true
            }

            if (binding.clientKeyPem.length() == 0) {
                binding.clientKeyPemLayout.error = getString(R.string.key_is_missing)
                hasValidationErrors = true
            }

            if (hasValidationErrors) {
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                model.saveConf {
                    it.copy(
                        serverUrl = binding.serverUrl.text.toString(),
                        serverCertPem = binding.serverCertPem.text.toString(),
                        clientCertPem = binding.clientCertPem.text.toString(),
                        clientKeyPem = binding.clientKeyPem.text.toString(),
                    )
                }

                val inputWidgets = listOf(
                    binding.serverUrlLayout,
                    binding.serverCertPemLayout,
                    binding.clientCertPemLayout,
                    binding.clientKeyPemLayout,
                    binding.scanBlitzQr,
                    binding.connect,
                )

                inputWidgets.forEach { it.isVisible = false }
                binding.progress.isVisible = true

                runCatching {
                    model.testConnection()
                }.onSuccess {
                    model.saveConf { it.copy(authCompleted = true) }
                    findNavController().navigate(R.id.authFragment_toPaymentsFragment)
                }.onFailure {
                    val message = getString(R.string.failed_to_connect_to_core_lightning)
                    Log.e("auth", message, it)
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }

                inputWidgets.forEach { it.isVisible = true }
                binding.progress.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}