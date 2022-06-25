package channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bubelov.thunder.databinding.ItemChannelBinding
import db.ListFundsChannel

class ChannelsAdapter : ListAdapter<ListFundsChannel, ChannelsAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(item: ListFundsChannel) = binding.apply {
            binding.channelId.text = item.shortChannelId + " (${item.ourAmountMsat / 1000}/${item.amountMsat / 1000})"
            val percent = (item.ourAmountMsat.toDouble() / item.amountMsat.toDouble() * 100).toInt()
            binding.progress.progress = percent
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ListFundsChannel>() {

        override fun areItemsTheSame(
            oldItem: ListFundsChannel,
            newItem: ListFundsChannel,
        ): Boolean {
            return false
        }

        override fun areContentsTheSame(
            oldItem: ListFundsChannel,
            newItem: ListFundsChannel,
        ): Boolean {
            return false
        }
    }
}