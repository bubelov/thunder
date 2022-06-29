package sync

import api.ApiController
import cln.NodeOuterClass
import db.Db
import db.ListFundsChannel
import db.ListFundsOutput
import db.ListFundsResponse
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
                val funds = withContext(Dispatchers.IO) {
                    listFunds(NodeOuterClass.ListfundsRequest.getDefaultInstance())
                }

                db.transaction {
                    db.listFundsResponseQueries.deleteAll()

                    db.listFundsResponseQueries.insert(
                        ListFundsResponse(
                            ZonedDateTime.now(ZoneOffset.UTC).toString()
                        )
                    )

                    db.listFundsOutputQueries.deleteAll()

                    funds.outputsList.forEach {
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

                    db.listFundsChannelQueries.deleteAll()

                    funds.channelsList.forEach {
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
            }

        }

        _state.update { State.Inactive }
    }

    sealed class State {
        object Inactive : State()
        object Active : State()
    }
}