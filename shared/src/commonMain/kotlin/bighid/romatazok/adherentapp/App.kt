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
import androidx.compose.runtime.setValue
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
import bighid.romatazok.adherentapp.ui.login.SampleLogin
import bighid.romatazok.adherentapp.ui.membership.MembershipScreen
import bighid.romatazok.adherentapp.ui.membership.SampleMembership
import bighid.romatazok.adherentapp.ui.receipt.ReceiptContent
import bighid.romatazok.adherentapp.ui.receipt.ReceiptView
import bighid.romatazok.adherentapp.ui.theme.ParibabiTheme
import kotlinx.coroutines.delay

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

/** Signed in with it, this phone receives a request from a new phone a few seconds later. */
private const val SAMPLE_OLD_PHONE = "0700000005"

/** Signed in with it, this phone is signed out a few seconds later by a change the team validated. */
private const val SAMPLE_TAKEN_OVER = "0700000006"

/** No tabs: everything starts from the home, one page at most, and every page comes back to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    ParibabiTheme(dark = isSystemInDarkTheme()) {
        var page by remember { mutableStateOf<Page>(Page.Home) }
        var editing by remember { mutableStateOf(false) }
        var receipt by remember { mutableStateOf<ReceiptView?>(null) }
        var event by remember { mutableStateOf<UpcomingEvent?>(null) }
        var message by remember { mutableStateOf<ActivityRow?>(null) }
        var home by remember { mutableStateOf(SampleHome) }
        // The number signed in on this phone; null shows the connection.
        var phone by remember { mutableStateOf<String?>(null) }
        var notice by remember { mutableStateOf<String?>(null) }
        var deviceRequest by remember { mutableStateOf(false) }
        var takenOver by remember { mutableStateOf(false) }
        var lockOnOpen by remember { mutableStateOf(false) }
        var locked by remember { mutableStateOf(false) }
        var changingPin by remember { mutableStateOf(false) }
        val login = remember { SampleLogin() }
        val signedIn = phone
        if (signedIn == null) {
            LoginScreen(login, notice, takenOver) {
                phone = it
                notice = null
                takenOver = false
                page = Page.Home
            }
            return@ParibabiTheme
        }
        LaunchedEffect(signedIn) {
            // Fictitious: this number plays the phone already connected, asked by a new one.
            if (signedIn == SAMPLE_OLD_PHONE) {
                delay(3_000)
                deviceRequest = true
            }
            // Fictitious: this one is signed out by a change the team validated, without its agreement.
            if (signedIn == SAMPLE_TAKEN_OVER) {
                delay(3_000)
                phone = null
                takenOver = true
                notice = "L'équipe a ouvert ton compte sur un autre téléphone aujourd'hui à 18:02."
            }
        }
        // Left in the background, the app asks the PIN again on return, when the member turned it on.
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (lockOnOpen) locked = true }
        if (locked) {
            LockScreen(login, signedIn, onUnlocked = { locked = false }, onSignOut = {
                locked = false
                phone = null
            })
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
                onLockOnOpen = { lockOnOpen = it },
                onChangePin = {
                    // A sheet in place of the other, never one over another.
                    editing = false
                    changingPin = true
                },
                onLogout = {
                    editing = false
                    phone = null
                },
            )
        }
        if (changingPin) {
            ChangePinSheet(login, signedIn, onDismiss = { changingPin = false }, onLocked = {
                changingPin = false
                phone = null
            })
        }
        if (deviceRequest) {
            DeviceRequestSheet(
                "Aujourd'hui à 17:42",
                onAccept = {
                    deviceRequest = false
                    phone = null
                    notice = "Ton compte est maintenant ouvert sur un autre téléphone."
                },
                onDismiss = { deviceRequest = false },
            )
        }
    }
}
