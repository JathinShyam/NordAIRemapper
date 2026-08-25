package com.nordairemapper.service.adb

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the 6-digit pairing code typed into the heads-up notification and
 * hands it to [PairingGrantService], which completes pairing + grants outside
 * the broadcast grace window (RCA 2026-08-25: doing the work inside goAsync()
 * hit the ~10s timeout mid-grants and killed commands #2/#3 silently).
 */
@AndroidEntryPoint
class PairingReplyReceiver : BroadcastReceiver() {

    @Inject lateinit var grantViaWirelessAdb: ReadLogsGrantViaWirelessAdb

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive: reply received")
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(PairingNotifier.KEY_CODE)
            ?.toString()
            ?.filter { it.isDigit() }
            .orEmpty()
        Log.i(TAG, "code digits=${code.length}")

        if (code.length != 6) {
            PairingNotifier.postPrompt(
                context,
                PairingSession.pairingPort,
                errorLine = "That wasn't 6 digits — enter the full code.",
            )
            return
        }

        if (!PairingSession.isActive) {
            PairingNotifier.postResult(
                context,
                ok = false,
                "Pairing session expired — open Keyforge and start again.",
            )
            return
        }

        val session = PairingSession.current()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // shortService FGS: exempt from background-start limits, minutes
            // of budget instead of the broadcast window.
            context.startForegroundService(
                PairingGrantService.intent(
                    context = context,
                    code = code,
                    host = session?.host,
                    pairingPort = session?.pairingPort,
                    connectPort = session?.connectPort,
                ),
            )
        } else {
            // API 33 fallback: no shortService type — keep the legacy in-window
            // path (quick bounds the flow so it fits the grace window).
            legacyInWindowPairing(context, code, session)
        }
    }

    private fun legacyInWindowPairing(
        context: Context,
        code: String,
        session: PairingSession.Snapshot?,
    ) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PairingNotifier.postProgress(appContext, "Pairing with the code you entered…")
                val result = grantViaWirelessAdb.pairAndGrant(
                    pairingCode = code,
                    host = session?.host,
                    pairingPort = session?.pairingPort,
                    connectPort = session?.connectPort,
                    quick = true,
                )
                PairingGrantService.handleResult(appContext, result)
            } catch (t: Throwable) {
                Log.w(TAG, "Reply pairing crashed", t)
                PairingNotifier.postResult(appContext, ok = false, t.message ?: t.javaClass.simpleName)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PairingReply"
    }
}
