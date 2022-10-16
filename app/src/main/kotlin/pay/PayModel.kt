package pay

import androidx.lifecycle.ViewModel
import api.ApiController
import cln.NodeOuterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PayModel(
    private val apiController: ApiController,
) : ViewModel() {

    suspend fun payBolt11(invoice: String): String {
        return withContext(Dispatchers.IO) {
            apiController.api.value!!.pay(
                NodeOuterClass.PayRequest.newBuilder()
                    .setBolt11(invoice)
                    .build()
            ).status.name
        }
    }
}