package sync

import android.util.Log
import api.ApiController
import cln.NodeOuterClass
import conf.ConfRepo
import db.Channel
import db.Db
import db.Output
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Single
class Sync(
    private val apiController: ApiController,
    private val confRepo: ConfRepo,
    private val db: Db,
) {

    private val _state = MutableStateFlow<State>(State.Inactive)
    val state = _state.asStateFlow()

    suspend fun sync() {
        while (_state.value != State.Inactive) {
            delay(100)
        }

        _state.update { State.Active }

        while (apiController.api.value == null) {
            delay(100)
        }

        apiController.api.value?.apply {
            runCatching {
                val transactions = listTransactions(NodeOuterClass.ListtransactionsRequest.getDefaultInstance())

                println(transactions.transactionsCount)

                val funds = withContext(Dispatchers.IO) {
                    listFunds(NodeOuterClass.ListfundsRequest.getDefaultInstance())
                }

                db.transaction {
                    db.outputQueries.deleteAll()

                    funds.outputsList.forEach {
                        db.outputQueries.insert(
                            Output(
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

                    db.channelQueries.deleteAll()

                    funds.channelsList.forEach {
                        db.channelQueries.insert(
                            Channel(
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

                confRepo.save { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC).toString()) }
            }.onFailure {
                Log.e("sync", "Failed to sync", it)
            }
        }

        _state.update { State.Inactive }
    }

    sealed class State {
        object Inactive : State()
        object Active : State()
    }
}