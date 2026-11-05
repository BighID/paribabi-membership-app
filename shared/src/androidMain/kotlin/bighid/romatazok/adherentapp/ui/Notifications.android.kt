package bighid.romatazok.adherentapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit

private const val PREFERENCES = "paribabi"
private const val ASKED = "notifications_asked"

/** Android 13 and later ask the member; before, notifications are allowed by the installation. */
@Composable
actual fun AskNotificationsOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (granted || preferences.getBoolean(ASKED, false)) return@LaunchedEffect
        preferences.edit { putBoolean(ASKED, true) }
        ask.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
