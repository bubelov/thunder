package channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.FragmentChannelsBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChannelsFragment : Fragment() {

    private val model: ChannelsModel by viewModel()

    private var _binding: FragmentChannelsBinding? = null
    private val binding get() = _binding!!

    private var adapter = ChannelsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.items.adapter = adapter
        binding.items.layoutManager = LinearLayoutManager(requireContext())

        val listItemDecoration = CardListAdapterDecoration(
            resources.getDimensionPixelSize(R.dimen.card_padding)
        )

        binding.items.addItemDecoration(listItemDecoration)

        model.state
            .onEach { binding.setState(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun FragmentChannelsBinding.setState(state: ChannelsModel.State?) {
        when (state) {
            null -> {
                progressIndicator.isVisible = true
                progressMessage.isVisible = false
                items.isVisible = false
            }

            is ChannelsModel.State.ConnectingToTor -> {
                progressIndicator.isVisible = true
                progressMessage.isVisible = true
                progressMessage.text = state.status
                items.isVisible = false
            }

            ChannelsModel.State.LoadingData -> {
                progressIndicator.isVisible = true
                progressMessage.isVisible = true
                progressMessage.text = getString(R.string.loading_data)
                items.isVisible = false
            }

            is ChannelsModel.State.DisplayingData -> {
                progressIndicator.isVisible = false
                progressMessage.isVisible = false
                items.isVisible = true
                adapter.submitList(state.channels)
            }
        }
    }
}