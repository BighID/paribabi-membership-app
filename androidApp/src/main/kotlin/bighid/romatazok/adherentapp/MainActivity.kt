package bighid.romatazok.adherentapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import bighid.romatazok.adherentapp.data.AndroidContext

/** A fragment activity, for the fingerprint or face prompt that opens the app. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The safe and the network need the application's context before the first screen.
        AndroidContext.app = applicationContext

        setContent {
            App(BuildConfig.SERVER_URL)
        }
    }
}
