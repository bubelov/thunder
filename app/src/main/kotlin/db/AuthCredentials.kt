package db

data class AuthCredentials(
    val serverUrl: String,
    val serverCertPem: String,
    val clientCertPem: String,
    val clientKeyPem: String,
)

fun Conf.authCredentials(): AuthCredentials {
    return AuthCredentials(
        serverUrl = serverUrl,
        serverCertPem = serverCertPem,
        clientCertPem = clientCertPem,
        clientKeyPem = clientKeyPem,
    )
}