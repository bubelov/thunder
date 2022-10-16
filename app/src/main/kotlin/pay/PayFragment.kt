package pay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import bech32.Bech32
import com.bubelov.thunder.databinding.FragmentPayBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.math.BigDecimal

class PayFragment : Fragment() {

    private val model: PayModel by inject()

    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    private val bolt11Scanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        binding.bolt11.setText(result.contents)

        val bech32Data = Bech32.decode(result.contents)

        if (!bech32Data.hrp.startsWith("lnbc")) {
            throw Exception()
        }

        val amountString = bech32Data.hrp.replace("lnbc", "")

        val amountBtc = if (amountString.toIntOrNull() != null) {
            amountString.toBigDecimal()
        } else {
            val multiplier = when (amountString.last()) {
                'm' -> BigDecimal.valueOf(0.001)
                'u' -> BigDecimal.valueOf(0.000001)
                'n' -> BigDecimal.valueOf(0.000000001)
                'p' -> BigDecimal.valueOf(0.000000000001)
                else -> throw Exception()
            }

            amountString.trim { !it.isDigit() }.toBigDecimal().multiply(multiplier)
        }

        val amountSats = amountBtc.multiply(BigDecimal.valueOf(100_000_000))

        binding.amount.setText(amountSats.toInt().toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.bolt11Layout.setEndIconOnClickListener { bolt11Scanner.launch(ScanOptions()) }

        binding.pay.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.scrollView.isVisible = false
                binding.pay.isVisible = false
                binding.progressBar.isVisible = true

                val status =
                    runCatching { model.payBolt11(binding.bolt11.text.toString()) }.getOrNull()

                AlertDialog.Builder(requireContext()).apply {
                    setMessage(status ?: "Exception")
                }.show()

                binding.progressBar.isVisible = false
                binding.scrollView.isVisible = true
                binding.pay.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}