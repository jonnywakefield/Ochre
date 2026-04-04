package com.ochre.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import com.ochre.app.MainActivity

/**
 * Transparent, no-history activity that receives NFC NDEF intents.
 *
 * Android delivers NFC intents to this activity when a matching tag is scanned
 * (whether the app is open or not). It reads the NDEF payload, dispatches the
 * action silently, then either finishes immediately or opens MainActivity for
 * actions that need UI (note, weight entry).
 *
 * To handle a tag scan when the app is already in the foreground, enable
 * foreground dispatch in MainActivity.onResume/onPause (see inline comments).
 */
class NfcDispatchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContentView — transparent, no UI

        NfcActionRegistry.registerAll(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) {
            finish()
            return
        }

        val payload = extractPayload(intent)
        if (payload == null) {
            finish()
            return
        }

        if (NfcActionRegistry.requiresAppOpen(payload)) {
            // Open MainActivity and let it handle the action via its own intent handling
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                action = payload          // e.g. "ochre:log_note"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(mainIntent)
        } else {
            val handled = NfcActionRegistry.dispatch(this, payload)
            if (!handled) {
                Toast.makeText(this, "Unknown tag", Toast.LENGTH_SHORT).show()
            }
        }

        finish()
    }

    /**
     * Reads the first NDEF text record from the intent parcelable array.
     * Returns the raw string payload, or null if the tag has no readable NDEF data.
     */
    private fun extractPayload(intent: Intent): String? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        val ndefMessage = rawMessages[0] as? NdefMessage ?: return null
        val record = ndefMessage.records.firstOrNull() ?: return null

        // NDEF text record: payload bytes start at offset 3 (1 status + 2 lang length)
        // We store plain ASCII so a simple string decode works
        return try {
            val payloadBytes = record.payload
            // Skip the status byte and language code (standard NDEF text record format)
            val langLength = payloadBytes[0].toInt() and 0x3F
            String(payloadBytes, 1 + langLength, payloadBytes.size - 1 - langLength, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
