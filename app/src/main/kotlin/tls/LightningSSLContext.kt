package tls

import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun lightningNodeSSLContext(
    serverCertPem: String,
    clientCertPem: String,
    clientKeyPem: String,
): SSLContext {
    val serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    serverKeyStore.load(null, null)
    serverKeyStore.setCertificateEntry("server-cert", serverCertPem.toX509Certificate())

    val clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    clientKeyStore.load(null, null)

    clientKeyStore.setKeyEntry(
        "client-key",
        clientKeyPem.toECPrivateKey(),
        "".toCharArray(),
        arrayOf(clientCertPem.toX509Certificate()),
    )

    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(clientKeyStore, "".toCharArray())
    val keyManagers = kmf.keyManagers

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(serverKeyStore)
    val trustManagers = trustManagerFactory.trustManagers
    val trustManager = trustManagers.first() as X509TrustManager

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagers, arrayOf(trustManager), null)

    return sslContext
}

private fun String.toX509Certificate(): X509Certificate {
    val factory = CertificateFactory.getInstance("X.509")
    return factory.generateCertificate(byteInputStream()) as X509Certificate
}

private fun String.toECPrivateKey(): ECPrivateKey {
    val pkcsEncodedKey = Base64.getDecoder().decode(
        this
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace(System.lineSeparator(), "")
            .replace("-----END PRIVATE KEY-----", "")
    )

    val keyFactory = KeyFactory.getInstance("EC")
    val keySpec = PKCS8EncodedKeySpec(pkcsEncodedKey)
    return keyFactory.generatePrivate(keySpec) as ECPrivateKey
}