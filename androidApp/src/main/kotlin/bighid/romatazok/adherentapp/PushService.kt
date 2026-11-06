package bighid.romatazok.adherentapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import bighid.romatazok.adherentapp.data.Push
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** The channel the server names in each notification: its name in the phone's settings. */
private const val CHANNEL = "paribabi"

/**
 * Firebase on this phone: a new token goes to the app, which sends it to the server once signed in. App closed or in
 * the background, Android shows the notification itself; open, this service shows it.
 */
class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Push.received(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        show(this, notification.title, notification.body, message.data)
    }

    companion object {
        /** The channel, created once, then the token Firebase holds already; nothing without google-services.json. */
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL, "PariBabi", NotificationManager.IMPORTANCE_HIGH)
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
            runCatching { FirebaseMessaging.getInstance().token.addOnSuccessListener(Push::received) }
        }

        private fun show(
            context: Context,
            title: String?,
            body: String?,
            data: Map<String, String>,
        ) {
            // The same extras as a notification shown by Android: the activity reads them the same way.
            val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            data.forEach { (key, value) -> intent.putExtra(key, value) }
            val id = System.currentTimeMillis().toInt()
            val open = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val built =
                NotificationCompat
                    .Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(0xFFEE6A2F.toInt())
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setContentIntent(open)
                    .build()
            // Refused by the member: nothing shown, nothing asked again.
            runCatching { NotificationManagerCompat.from(context).notify(id, built) }
        }
    }
}
