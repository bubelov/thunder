package payments

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import api.ApiController
import cln.NodeOuterClass
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import db.Db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import sync.Sync

@KoinViewModel
class PaymentsModel(
    private val apiController: ApiController,
    private val db: Db,
    sync: Sync,
) : ViewModel() {

    private val _state = MutableStateFlow<State?>(null)
    val state = _state.asStateFlow()

    init {
        combine(
            sync.state,
            db.listFundsResponseQueries.select().asFlow().mapToOneOrNull(),
        ) { _, listFundsResponse ->
            _state.update { State.LoadingData }

            if (listFundsResponse != null) {
                val channels = db.listFundsChannelQueries.selectAll().asFlow().mapToList().first()
                _state.update { State.DisplayingData(channels.sumOf { it.ourAmountMsat } / 1000) }
            }
        }.launchIn(viewModelScope)
    }

    suspend fun pay(invoice: String) {
        Log.d("payments", "Paying invoice $invoice")

        val response = withContext(Dispatchers.Default) {
            apiController.api.value!!.pay(
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