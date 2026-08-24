package com.nordairemapper.service.adb

/**
 * Holds the current Wireless-debugging pairing endpoint while the Unlock
 * screen's Built-In flow is active, so [PairingReplyReceiver] can complete
 * pairing straight from the notification reply — even while a system dialog
 * covers the app.
 */
object PairingSession {

    @Volatile var host: String? = null
        private set
    @Volatile var pairingPort: Int? = null
        private set
    @Volatile var connectPort: Int? = null
        private set

    val isActive: Boolean get() = pairingPort != null || host != null

    fun set(host: String?, pairingPort: Int?, connectPort: Int?) {
        this.host = host?.let { ReadLogsGrantViaWirelessAdb.normalizeHost(it) }?.ifEmpty { null }
        this.pairingPort = pairingPort?.takeIf { it > 0 }
        this.connectPort = connectPort?.takeIf { it > 0 }
    }

    fun clear() {
        host = null
        pairingPort = null
        connectPort = null
    }
}
