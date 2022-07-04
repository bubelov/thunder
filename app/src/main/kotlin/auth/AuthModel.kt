package auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import api.ApiBuilder
import cln.NodeOuterClass
import conf.ConfRepo
import db.Conf
import db.authCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import tor.TorController

@KoinViewModel
class AuthModel(
    private val torController: TorController,
    private val confRepo: ConfRepo,
) : ViewModel() {

    fun saveServerUrl(url: String) {
        viewModelScope.launch { confRepo.save { it.copy(serverUrl = url) } }
    }

    fun saveServerCert(cert: String) {
        viewModelScope.launch { confRepo.save { it.copy(serverCertPem = cert) } }
    }

    fun saveClientCert(cert: String) {
        viewModelScope.launch { confRepo.save { it.copy(clientCertPem = cert) } }
    }

    fun saveClientKey(key: String) {
        viewModelScope.launch { confRepo.save { it.copy(clientKeyPem = key) } }
    }

    suspend fun saveConf(applyChanges: (Conf) -> Conf) = confRepo.save(applyChanges)

    suspend fun testConnection() {
        withContext(Dispatchers.Default) {
            val api = ApiBuilder().build(
                creds = confRepo.load().first().authCredentials(),
                torController = torController,
            )

            api.listFunds(NodeOuterClass.ListfundsRequest.getDefaultInstance())
        }
    }
}