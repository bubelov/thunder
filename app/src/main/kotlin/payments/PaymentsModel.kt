package payments

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.App
import cln.NodeOuterClass
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import db.Db
import db.ListFundsChannel
import db.ListFundsOutput
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
class PaymentsModel(
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
                _state.update { State.DisplayingData(channels.sumOf { it.ourAmountMsat } / 1000) }
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

                        res.outputsList.forEach {
                            db.listFundsOutputQueries.insert(
                                ListFundsOutput(
                                    txid = it.txid.toStringUtf8(),
                                    output = it.output.toLong(),
                                    amountMsat = it.amountMsat.msat,
                                    scriptPubKey = it.scriptpubkey.toStringUtf8(),
                                    status = it.status.name,
                                    reserved = it.reserved,
                                    address = it.address,
                                    redeemScript = it.redeemscript.toStringUtf8(),
                                    blockHeight = it.blockheight.toLong(),
                                )
                            )
                        }

                        res.channelsList.forEach {
                            db.listFundsChannelQueries.insert(
                                ListFundsChannel(
                                    peerId = it.peerId.toStringUtf8(),
                                    ourAmountMsat = it.ourAmountMsat.msat,
                                    amountMsat = it.amountMsat.msat,
                                    fundingTxid = it.fundingTxid.toStringUtf8(),
                                    fundingOutput = it.fundingOutput.toLong(),
                                    connected = it.connected,
                                    state = it.state.name,
                                    shortChannelId = it.shortChannelId,
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

    suspend fun pay(invoice: String) {
        Log.d("payments", "Paying invoice $invoice")

        val response = withContext(Dispatchers.Default) {
            getApplication<App>().node.value!!.pay(
                NodeOuterClass.PayRequest.newBuilder()
                    .setBolt11(invoice)
                    .build()
            )
        }

        Log.d("payments", "Response status ${response.status}")
    }

    sealed class State {
        data class ConnectingToTor(val status: String) : State()
        object LoadingData : State()
        data class DisplayingData(val totalSats: Long) : State()
    }
}