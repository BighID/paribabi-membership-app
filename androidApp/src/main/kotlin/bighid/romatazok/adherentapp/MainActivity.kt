package bighid.romatazok.adherentapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import bighid.romatazok.adherentapp.data.AndroidContext
import bighid.romatazok.adherentapp.data.DeepLink
import bighid.romatazok.adherentapp.data.Push

/**
 * A fragment activity, for the fingerprint or face prompt that opens the app. One at a time: a tapped notification
 * or a `paribabi://` link reaches the one already open.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The safe and the network need the application's context before the first screen.
        AndroidContext.app = applicationContext
        PushService.start(applicationContext)
        // Not again when the phone rotates: the link was already opened.
        if (savedInstanceState == null) follow(intent)

        setContent {
            App(BuildConfig.SERVER_URL)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        follow(intent)
    }

    /** What a notification carries in its extras (app closed, Android shows it), or the link that opened the app. */
    private fun follow(intent: Intent?) {
        intent ?: return
        Push.open(DeepLink.fromUri(intent.dataString) ?: DeepLink.fromNotification(intent.getStringExtra("target"), intent.getStringExtra("id")))
    }
}
