package db

data class AuthCredentials(
    val serverUrl: String,
    val serverCertificate: String,
    val clientCertificate: String,
    val clientPrivateKey: String,
)

fun Conf.authCredentials(): AuthCredentials {
    return AuthCredentials(
        serverUrl = serverUrl,
        serverCertificate = serverCertificate,
        clientCertificate = clientCertificate,
        clientPrivateKey = clientPrivateKey,
    )
}