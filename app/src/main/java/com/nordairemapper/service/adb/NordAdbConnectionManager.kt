package com.nordairemapper.service.adb

import android.content.Context
import android.os.Build
import android.sun.security.x509.CertAndKeyGen
import android.sun.security.x509.X500Name
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.atomic.AtomicReference

/**
 * Persists an ADB RSA key/cert in app-private storage for Wireless Debugging pairing.
 */
class NordAdbConnectionManager private constructor(
    private val privateKey: PrivateKey,
    private val certificate: Certificate,
) : AbsAdbConnectionManager() {

    init {
        setApi(Build.VERSION.SDK_INT)
    }

    override fun getPrivateKey(): PrivateKey = privateKey

    override fun getCertificate(): Certificate = certificate

    override fun getDeviceName(): String = DEVICE_NAME

    companion object {
        private const val DEVICE_NAME = "NordAIRemapper"
        private const val DIR_NAME = "adb"
        private const val KEY_FILE = "adbkey.pk8"
        private const val CERT_FILE = "adbkey.crt"
        private const val SUBJECT = "CN=Keyforge"
        private const val KEY_SIZE = 2048
        /** Cert validity in seconds (10 years). */
        private const val CERT_VALIDITY_SECONDS = 10L * 365L * 24L * 60L * 60L

        private val instanceRef = AtomicReference<NordAdbConnectionManager?>()

        fun getInstance(context: Context): NordAdbConnectionManager {
            instanceRef.get()?.let { return it }
            synchronized(this) {
                instanceRef.get()?.let { return it }
                val created = create(context.applicationContext)
                instanceRef.set(created)
                return created
            }
        }

        private fun create(context: Context): NordAdbConnectionManager {
            val dir = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
            val keyFile = File(dir, KEY_FILE)
            val certFile = File(dir, CERT_FILE)
            if (keyFile.isFile && certFile.isFile) {
                runCatching {
                    val key = KeyFactory.getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
                    val cert = CertificateFactory.getInstance("X.509")
                        .generateCertificate(certFile.inputStream())
                    return NordAdbConnectionManager(key, cert)
                }
            }
            return generateAndPersist(keyFile, certFile)
        }

        private fun generateAndPersist(keyFile: File, certFile: File): NordAdbConnectionManager {
            val certAndKeyGen = CertAndKeyGen("RSA", "SHA512withRSA")
            certAndKeyGen.setRandom(SecureRandom())
            certAndKeyGen.generate(KEY_SIZE)
            val privateKey = certAndKeyGen.privateKey
            val certificate = certAndKeyGen.getSelfCertificate(
                X500Name(SUBJECT),
                CERT_VALIDITY_SECONDS,
            )

            keyFile.writeBytes(privateKey.encoded)
            certFile.writeBytes(certificate.encoded)

            return NordAdbConnectionManager(privateKey, certificate)
        }
    }
}
