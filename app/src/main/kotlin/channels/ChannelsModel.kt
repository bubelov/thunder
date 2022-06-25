package channels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.App
import cln.NodeOuterClass
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import db.Db
import db.ListFundsChannel
import db.ListFundsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.time.ZoneOffset
import java.time.ZonedDateTime

@KoinViewModel
class ChannelsModel(
    app: Application,
    private val db: Db,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<State?>(null)
    val state = _state.asStateFlow()

    init {
        combine(
            getApplication<App>().torConnectionStatus,
            getApplication<App>().node,
            db.listFundsResponseQueries.select().asFlow().mapToOneOrNull(),
        ) { torConnectionStatus, node, listFundsResponse ->
            if (listFundsResponse != null) {
                _state.update { State.LoadingData }
                val channels =
                    withContext(Dispatchers.Default) { db.listFundsChannelQueries.selectAll().executeAsList() }
                _state.update { State.DisplayingData(channels) }
            } else {
                if (node != null) {
                    _state.update { State.LoadingData }

                    val res = withContext(Dispatchers.IO) {
                        node.listFunds(NodeOuterClass.ListfundsRequest.getDefaultInstance())
                    }

                    db.transaction {
                        db.listFundsResponseQueries.deleteAll()

                        db.listFundsResponseQueries.insert(
                            ListFundsResponse(
                                ZonedDateTime.now(ZoneOffset.UTC).toString()
                            )
                        )

                        res.channelsList.forEach {
                            db.listFundsChannelQueries.insert(
                                ListFundsChannel(
                                    ourAmountMsat = it.ourAmountMsat.msat,
                                    amountMsat = it.amountMsat.msat,
                                )
                            )
                        }
                    }
                } else {
                    _state.update { State.ConnectingToTor(torConnectionStatus) }
                }
            }
        }.launchIn(viewModelScope)
    }

    sealed class State {
        data class ConnectingToTor(val status: String) : State()
        object LoadingData : State()
        data class DisplayingData(val channels: List<ListFundsChannel>) : State()
    }
}