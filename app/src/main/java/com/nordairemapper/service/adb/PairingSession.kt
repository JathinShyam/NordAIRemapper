package com.nordairemapper.service.adb

/**
 * Holds the current Wireless-debugging pairing endpoint while the Unlock
 * screen's Built-In flow is active, so [PairingReplyReceiver] can complete
 * pairing straight from the notification reply — even while a system dialog
 * covers the app.
 *
 * State is one immutable snapshot swapped atomically: a concurrent re-arm
 * (watch found a newer port while a reply was in flight) can never yield a
 * torn host/port mix.
 */
object PairingSession {

    data class Snapshot(
        val host: String?,
        val pairingPort: Int?,
        val connectPort: Int?,
    )

    @Volatile
    private var snapshot: Snapshot? = null

    val isActive: Boolean get() = snapshot != null

    /** Point-in-time view for the reply path. */
    fun current(): Snapshot? = snapshot

    fun set(host: String?, pairingPort: Int?, connectPort: Int?) {
        val normalizedHost = host
            ?.let { ReadLogsGrantViaWirelessAdb.normalizeHost(it) }
            ?.ifEmpty { null }
        snapshot = Snapshot(
            host = normalizedHost,
            pairingPort = pairingPort?.takeIf { it > 0 },
            connectPort = connectPort?.takeIf { it > 0 },
        )
    }

    fun clear() {
        snapshot = null
    }

    // Convenience views (read together via current() where atomicity matters).
    val host: String? get() = snapshot?.host
    val pairingPort: Int? get() = snapshot?.pairingPort
    val connectPort: Int? get() = snapshot?.connectPort
}
