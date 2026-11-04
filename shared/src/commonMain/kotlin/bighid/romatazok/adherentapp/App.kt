package bighid.romatazok.adherentapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import bighid.romatazok.adherentapp.data.ApiLoginService
import bighid.romatazok.adherentapp.data.MemberApi
import bighid.romatazok.adherentapp.data.PendingRequest
import bighid.romatazok.adherentapp.data.SessionExpiredException
import bighid.romatazok.adherentapp.data.platform
import bighid.romatazok.adherentapp.ui.Page
import bighid.romatazok.adherentapp.ui.SystemBack
import bighid.romatazok.adherentapp.ui.activity.ActivityScreen
import bighid.romatazok.adherentapp.ui.activity.MessageSheet
import bighid.romatazok.adherentapp.ui.activity.SampleActivity
import bighid.romatazok.adherentapp.ui.events.EventSheet
import bighid.romatazok.adherentapp.ui.events.EventsScreen
import bighid.romatazok.adherentapp.ui.events.SampleEvents
import bighid.romatazok.adherentapp.ui.events.UpcomingEvent
import bighid.romatazok.adherentapp.ui.home.ActivityRow
import bighid.romatazok.adherentapp.ui.home.HomeScreen
import bighid.romatazok.adherentapp.ui.home.MemberStatus
import bighid.romatazok.adherentapp.ui.home.ProfileSheet
import bighid.romatazok.adherentapp.ui.home.SampleHome
import bighid.romatazok.adherentapp.ui.home.Target
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import bighid.romatazok.adherentapp.ui.login.ChangePinSheet
import bighid.romatazok.adherentapp.ui.login.DeviceRequestSheet
import bighid.romatazok.adherentapp.ui.login.LockScreen
import bighid.romatazok.adherentapp.ui.login.LoginScreen
import bighid.romatazok.adherentapp.ui.membership.MembershipScreen
import bighid.romatazok.adherentapp.ui.membership.SampleMembership
import bighid.romatazok.adherentapp.ui.receipt.ReceiptContent
import bighid.romatazok.adherentapp.ui.receipt.ReceiptView
import bighid.romatazok.adherentapp.ui.theme.ParibabiTheme
import kotlinx.coroutines.launch

/** Fictitious receipts of the home activity, until the screen is plugged to the server. */
private val SampleReceipts =
    mapOf(
        "r1" to ReceiptView("R-2026-00430", "Entrée · Soirée des adhérents", "5 000 F", "1 novembre 2026 à 21:12", "Espèces"),
        "r2" to ReceiptView("R-2026-00301", "Renouvellement PariBabi · Saison 2026-2027", "40 000 F", "2 septembre 2026 à 18:40", "Wave"),
        "r3" to ReceiptView("R-2026-00412", "Billet · Forum de l'entrepreneuriat", "Gratuit", "16 octobre 2026 à 19:44", "Aucun"),
        "r4" to ReceiptView("R-2026-00188", "Entrée · Gala de fin de saison", "15 000 F", "28 juin 2026 à 20:31", "Orange Money"),
        "r5" to ReceiptView("R-2026-00142", "Entrée · Afterwork de mai", "5 000 F", "16 mai 2026 à 19:20", "Espèces"),
        "r6" to ReceiptView("R-2026-00097", "Entrée · Dîner des adhérents de mars", "10 000 F", "14 mars 2026 à 20:05", "Wave"),
        "r7" to ReceiptView("R-2026-00031", "Entrée · Afterwork de janvier", "5 000 F", "17 janvier 2026 à 19:12", "Espèces"),
        "r8" to ReceiptView("R-2025-00412", "Adhésion PariBabi · Saison 2025-2026", "40 000 F", "18 octobre 2025 à 11:40", "Wave"),
    )

/** What the phone's safe keeps besides the session: the number signed in, the lock on opening. */
private const val PHONE_KEY = "phone"
private const val LOCK_KEY = "lock_on_open"

/**
 * No tabs: everything starts from the home, one page at most, and every page comes back to it. [serverUrl] empty:
 * the development machine, as the emulator or the simulator sees it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(serverUrl: String = "") {
    ParibabiTheme(dark = isSystemInDarkTheme()) {
        val platform = remember { platform() }
        val secure = platform.secure
        val api = remember { MemberApi(platform.httpEngine(), secure, serverUrl.ifBlank { platform.defaultServerUrl }) }
        val login = remember { ApiLoginService(api, secure, platform.deviceModel) }
        val scope = rememberCoroutineScope()
        var page by remember { mutableStateOf<Page>(Page.Home) }
        var editing by remember { mutableStateOf(false) }
        var receipt by remember { mutableStateOf<ReceiptView?>(null) }
        var event by remember { mutableStateOf<UpcomingEvent?>(null) }
        var message by remember { mutableStateOf<ActivityRow?>(null) }
        var home by remember { mutableStateOf(SampleHome) }
        // The number signed in on this phone, kept with its session; null shows the connection.
        var phone by remember { mutableStateOf(secure.get(PHONE_KEY)?.takeIf { api.hasSession }) }
        var notice by remember { mutableStateOf<String?>(null) }
        var deviceRequest by remember { mutableStateOf<PendingRequest?>(null) }
        var lockOnOpen by remember { mutableStateOf(secure.get(LOCK_KEY) == "1") }
        // Opened again with a session and the lock asked for: the PIN first.
        var locked by remember { mutableStateOf(phone != null && lockOnOpen) }
        var changingPin by remember { mutableStateOf(false) }
        // Counts the returns to the foreground, to ask the server again.
        var resumed by remember { mutableStateOf(0) }
        val signedOut: (String?) -> Unit = { said ->
            api.forget()
            phone = null
            notice = said
            locked = false
            deviceRequest = null
            // The lock was this member's choice: the next one on this phone chooses again.
            lockOnOpen = false
            secure.remove(LOCK_KEY)
        }
        val signedIn = phone
        if (signedIn == null) {
            LoginScreen(login, notice) {
                secure.put(PHONE_KEY, it)
                phone = it
                notice = null
                page = Page.Home
            }
            return@ParibabiTheme
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumed++ }
        LaunchedEffect(signedIn, resumed) {
            // A new phone may be waiting for this one's yes; a session over sends back to the connection.
            try {
                deviceRequest = api.pendingRequests().firstOrNull()
            } catch (_: SessionExpiredException) {
                signedOut("Ta session a pris fin. Reconnecte-toi.")
            } catch (_: Exception) {
                // No network: asked again at the next return.
            }
        }
        // Left in the background, the app asks the PIN again on return, when the member turned it on.
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (lockOnOpen) locked = true }
        if (locked) {
            LockScreen(login, onUnlocked = { locked = false }, onSignOut = { signedOut(null) })
            return@ParibabiTheme
        }
        SystemBack(page != Page.Home) { page = Page.Home }
        AnimatedContent(
            page,
            transitionSpec = {
                if (targetState == Page.Home) {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                }
            },
        ) { current ->
            val back = { page = Page.Home }
            when (current) {
                Page.Home ->
                    HomeScreen(
                        home,
                        onMembership = { page = Page.Membership },
                        // An event never takes a page from the home: it opens over it.
                        onEvent = { id -> event = SampleEvents.firstOrNull { it.id == id } },
                        onAllEvents = { page = Page.Events },
                        onActivity = { row ->
                            when (val target = row.target) {
                                is Target.Event -> event = SampleEvents.firstOrNull { it.id == target.id }
                                // A receipt never takes a page: it opens over the home.
                                is Target.Receipt -> receipt = SampleReceipts[target.code]
                                Target.Membership -> page = Page.Membership
                            }
                        },
                        onAllActivity = { page = Page.Activity },
                        onEditProfile = { editing = true },
                    )
                Page.Membership -> MembershipScreen(SampleMembership, back)
                Page.Events -> EventsScreen(SampleEvents, back) { event = it }
                Page.Activity ->
                    ActivityScreen(SampleActivity, back) { row ->
                        when (val target = row.target) {
                            is Target.Event -> event = SampleEvents.firstOrNull { it.id == target.id }
                            is Target.Receipt -> receipt = SampleReceipts[target.code]
                            // From the activity, a message opens over the page: the membership would be a second page.
                            Target.Membership -> message = row
                        }
                    }
            }
        }
        message?.let {
            MessageSheet(it, onDismiss = { message = null }, onMembership = {
                message = null
                page = Page.Membership
            })
        }
        event?.let {
            EventSheet(it, memberActive = home.status is MemberStatus.Active, onDismiss = { event = null }, onRenew = {
                event = null
                page = Page.Membership
            })
        }
        receipt?.let {
            ModalBottomSheet({ receipt = null }, containerColor = MaterialTheme.colorScheme.surface) { ReceiptContent(it, onDownload = {}) }
        }
        if (editing) {
            ProfileSheet(
                home.profile,
                onDismiss = { editing = false },
                onSave = { profile ->
                    home = home.copy(profile = profile, card = home.card.with(profile))
                    editing = false
                },
                lockOnOpen = lockOnOpen,
                onLockOnOpen = {
                    lockOnOpen = it
                    secure.put(LOCK_KEY, if (it) "1" else "0")
                },
                onChangePin = {
                    // A sheet in place of the other, never one over another.
                    editing = false
                    changingPin = true
                },
                onLogout = {
                    editing = false
                    scope.launch { api.logout() }
                    signedOut(null)
                },
            )
        }
        if (changingPin) {
            ChangePinSheet(login, onDismiss = { changingPin = false }, onLocked = {
                changingPin = false
                signedOut(null)
            })
        }
        deviceRequest?.let { request ->
            DeviceRequestSheet(
                "Depuis ${request.label ?: "un nouveau téléphone"}",
                onAccept = {
                    scope.launch {
                        runCatching { api.answerRequest(request.id, accept = true) }
                            .onSuccess { signedOut("Ton compte est maintenant ouvert sur un autre téléphone.") }
                    }
                },
                onRefuse = { scope.launch { runCatching { api.answerRequest(request.id, accept = false) } } },
                onDismiss = { deviceRequest = null },
            )
        }
    }
}
