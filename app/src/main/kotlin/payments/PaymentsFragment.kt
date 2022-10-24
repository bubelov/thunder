package payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.FragmentPaymentsBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.NumberFormat

class PaymentsFragment : Fragment() {

    private val model: PaymentsModel by viewModel()

    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!

    private val invoiceScanner = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                model.pay(result.contents)
                Toast.makeText(requireContext(), "Paid!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val adapter = PaymentsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.state
            .onEach { binding.setState(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.send.setOnClickListener {
            findNavController().navigate(R.id.paymentsFragment_toPayFragment)
        }
    }

    fun FragmentPaymentsBinding.setState(state: PaymentsModel.State?) {
        when (state) {
            null -> {
                progressIndicator.isVisible = true
                progressMessage.isVisible = false
                send.isVisible = false
            }

            is PaymentsModel.State.ConnectingToTor -> {
                progressIndicator.isVisible = true
                progressMessage.isVisible = true
                progressMessage.text = state.status
                send.isVisible = false
            }

            PaymentsModel.State.LoadingData -> {
                progressIndicator.isVisible = true
                progressMessage.isVisible = true
                progressMessage.text = getString(R.string.loading_data)
                send.isVisible = false
            }

            is PaymentsModel.State.DisplayingData -> {
                progressIndicator.isVisible = false
                progressMessage.isVisible = false
                binding.toolbar.title = getString(
                    R.string.s_sats,
                    NumberFormat.getNumberInstance().format(state.totalSats),
                )
                binding.send.isVisible = true

                binding.list.layoutManager = LinearLayoutManager(requireContext())
                binding.list.adapter = adapter
                adapter.submitList(state.payments)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}