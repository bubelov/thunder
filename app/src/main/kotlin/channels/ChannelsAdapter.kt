package channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cln.NodeOuterClass.ListfundsChannels
import com.bubelov.thunder.databinding.ItemChannelBinding

class ChannelsAdapter : ListAdapter<ListfundsChannels, ChannelsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemChannelBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListfundsChannels) = binding.apply {
            binding.channelId.text = item.shortChannelId + " (${item.ourAmountMsat.msat / 1000}/${item.amountMsat.msat / 1000})"
            val percent = (item.ourAmountMsat.msat.toDouble() / item.amountMsat.msat.toDouble() * 100).toInt()
            binding.progress.progress = percent
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ListfundsChannels>() {

        override fun areItemsTheSame(
            oldItem: ListfundsChannels,
            newItem: ListfundsChannels,
        ): Boolean {
            return false
        }

        override fun areContentsTheSame(
            oldItem: ListfundsChannels,
            newItem: ListfundsChannels,
        ): Boolean {
            return false
        }
    }
}