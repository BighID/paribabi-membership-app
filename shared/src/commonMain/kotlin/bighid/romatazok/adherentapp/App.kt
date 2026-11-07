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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import bighid.romatazok.adherentapp.data.ApiException
import bighid.romatazok.adherentapp.data.ApiLoginService
import bighid.romatazok.adherentapp.data.DeepLink
import bighid.romatazok.adherentapp.data.MemberApi
import bighid.romatazok.adherentapp.data.MemberRepository
import bighid.romatazok.adherentapp.data.PendingRequest
import bighid.romatazok.adherentapp.data.Push
import bighid.romatazok.adherentapp.data.SessionExpiredException
import bighid.romatazok.adherentapp.data.platform
import bighid.romatazok.adherentapp.ui.AskNotificationsOnce
import bighid.romatazok.adherentapp.ui.Loading
import bighid.romatazok.adherentapp.ui.Page
import bighid.romatazok.adherentapp.ui.SystemBack
import bighid.romatazok.adherentapp.ui.activity.ActivityScreen
import bighid.romatazok.adherentapp.ui.activity.MessageSheet
import bighid.romatazok.adherentapp.ui.events.EventSheet
import bighid.romatazok.adherentapp.ui.events.EventsScreen
import bighid.romatazok.adherentapp.ui.events.LocalPosters
import bighid.romatazok.adherentapp.ui.events.UpcomingEvent
import bighid.romatazok.adherentapp.ui.home.ActivityRow
import bighid.romatazok.adherentapp.ui.home.EmailChangeSheet
import bighid.romatazok.adherentapp.ui.home.HomeScreen
import bighid.romatazok.adherentapp.ui.home.HomeState
import bighid.romatazok.adherentapp.ui.home.MemberStatus
import bighid.romatazok.adherentapp.ui.home.Target
import bighid.romatazok.adherentapp.ui.login.ChangePinSheet
import bighid.romatazok.adherentapp.ui.login.DeviceRequestSheet
import bighid.romatazok.adherentapp.ui.login.LockScreen
import bighid.romatazok.adherentapp.ui.login.LoginScreen
import bighid.romatazok.adherentapp.ui.login.wrongPin
import bighid.romatazok.adherentapp.ui.membership.MembershipScreen
import bighid.romatazok.adherentapp.ui.membership.MembershipState
import bighid.romatazok.adherentapp.ui.membership.MembershipTab
import bighid.romatazok.adherentapp.ui.membership.ProfileActions
import bighid.romatazok.adherentapp.ui.receipt.LocalReceipts
import bighid.romatazok.adherentapp.ui.receipt.ReceiptPanel
import bighid.romatazok.adherentapp.ui.theme.ParibabiTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What the phone's safe keeps besides the session: the number signed in, the lock on opening. */
private const val PHONE_KEY = "phone"
private const val LOCK_KEY = "lock_on_open"

private const val TAKEN_OVER = "L'équipe a ouvert ton compte sur un autre téléphone."

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
        val data = remember { MemberRepository(api) }
        val scope = rememberCoroutineScope()
        val uri = LocalUriHandler.current
        var page by remember { mutableStateOf<Page>(Page.Home) }
        var membershipTab by remember { mutableStateOf(MembershipTab.SEASON) }
        // The payment whose receipt is open: the sheet opens at once and reads it.
        var receipt by remember { mutableStateOf<String?>(null) }
        var event by remember { mutableStateOf<UpcomingEvent?>(null) }
        var message by remember { mutableStateOf<ActivityRow?>(null) }
        var newEmail by remember { mutableStateOf<String?>(null) }
        // What the server said, kept while the app is open; the home waits for the first answer.
        var home by remember { mutableStateOf<HomeState?>(null) }
        var activity by remember { mutableStateOf<List<ActivityRow>>(emptyList()) }
        var events by remember { mutableStateOf<List<UpcomingEvent>>(emptyList()) }
        var membership by remember { mutableStateOf<MembershipState?>(null) }
        var membershipFailed by remember { mutableStateOf(false) }
        // Counts the asks to read the membership again.
        var membershipTry by remember { mutableStateOf(0) }
        var loadFailed by remember { mutableStateOf(false) }
        // The number signed in on this phone, kept with its session; null shows the connection.
        var phone by remember { mutableStateOf(secure.get(PHONE_KEY)?.takeIf { api.hasSession }) }
        var notice by remember { mutableStateOf<String?>(null) }
        var deviceRequest by remember { mutableStateOf<PendingRequest?>(null) }
        var lockOnOpen by remember { mutableStateOf(secure.get(LOCK_KEY) == "1") }
        // Opened again with a session and the lock asked for: the PIN first.
        var locked by remember { mutableStateOf(phone != null && lockOnOpen) }
        var changingPin by remember { mutableStateOf(false) }
        // Counts the returns to the foreground, and the asks to read again, to ask the server again.
        var resumed by remember { mutableStateOf(0) }
        // Pulled down on the home: the ring stays until the reading ends.
        var refreshing by remember { mutableStateOf(false) }
        val signedOut: (String?) -> Unit = { said ->
            api.forget()
            phone = null
            notice = said
            locked = false
            deviceRequest = null
            home = null
            membership = null
            // The lock was this member's choice: the next one on this phone chooses again.
            lockOnOpen = false
            secure.remove(LOCK_KEY)
        }
        val signedIn = phone
        if (signedIn == null) {
            // A notification for the member who left: after the connection, the home.
            LaunchedEffect(Unit) { Push.opened() }
            // Signed out by a change the team validated: the notice comes with "Ce n'était pas moi", for 7 days.
            val takenOver = api.takenOver
            LoginScreen(login, notice ?: if (takenOver) TAKEN_OVER else null, takenOver) {
                secure.put(PHONE_KEY, it)
                phone = it
                notice = null
                page = Page.Home
                // Back in: the report is no longer offered.
                api.dropTakeover()
            }
            return@ParibabiTheme
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumed++ }
        LaunchedEffect(signedIn, resumed) {
            // The member's data, and a new phone waiting for this one's yes; a session over sends back to the connection.
            try {
                val (read, rows, upcoming) = data.home()
                home = read
                activity = rows
                events = upcoming
                loadFailed = false
                // Read again behind what is shown: the page never goes back to its wheel.
                membershipTry++
                deviceRequest = api.pendingRequests().firstOrNull()
            } catch (_: SessionExpiredException) {
                signedOut(if (api.takenOver) TAKEN_OVER else "Ta session a pris fin. Reconnecte-toi.")
            } catch (_: Exception) {
                // No network: what was read stays; the first time, the home says so.
                loadFailed = true
            } finally {
                refreshing = false
            }
        }
        // The phone's notification token, sent once signed in and each time Firebase renews it; signing out
        // removes it on the server, so it goes again at the next sign-in.
        val pushToken by Push.token.collectAsState()
        LaunchedEffect(signedIn, pushToken) {
            pushToken?.let { token -> runCatching { api.registerPush(token) } }
        }
        LaunchedEffect(page, membershipTry) {
            if (page == Page.Membership) {
                membershipFailed = false
                val read = runCatching { data.membership() }.getOrNull()
                if (read != null) membership = read
                // Without network, what was read stays.
                membershipFailed = membership == null
            }
        }
        // Left in the background, the app asks the PIN again on return, when the member turned it on.
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (lockOnOpen) locked = true }
        if (locked) {
            LockScreen(
                login,
                onUnlocked = { locked = false },
                onSignOut = { signedOut(null) },
                onSessionOver = { signedOut(if (api.takenOver) TAKEN_OVER else "Ta session a pris fin. Reconnecte-toi.") },
            )
            return@ParibabiTheme
        }
        val shown = home
        if (shown == null) {
            Loading(failed = loadFailed, onRetry = { resumed++ })
            return@ParibabiTheme
        }
        // On the home, the member knows the app: the moment to ask for notifications, once.
        AskNotificationsOnce()
        // A tapped notification: shown here only, once signed in, past the PIN, with the home read.
        val link by Push.opening.collectAsState()
        LaunchedEffect(link) {
            when (val target = link) {
                is DeepLink.Receipt -> {
                    page = Page.Home
                    event = null
                    message = null
                    receipt = target.paymentId
                }
                DeepLink.Membership -> {
                    receipt = null
                    event = null
                    message = null
                    membershipTab = MembershipTab.SEASON
                    page = Page.Membership
                }
                null -> return@LaunchedEffect
            }
            Push.opened()
        }
        val openReceipt: (String) -> Unit = { id -> receipt = id }
        CompositionLocalProvider(LocalReceipts provides data, LocalPosters provides data::poster) {
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
                            shown,
                            onMembership = {
                                membershipTab = MembershipTab.SEASON
                                page = Page.Membership
                            },
                            // An event never takes a page from the home: it opens over it.
                            onEvent = { id -> event = events.firstOrNull { it.id == id } },
                            onAllEvents = { page = Page.Events },
                            onActivity = { row ->
                                when (val target = row.target) {
                                    is Target.Event -> event = events.firstOrNull { it.id == target.id }
                                    // A receipt never takes a page: it opens over the home.
                                    is Target.Receipt -> openReceipt(target.code)
                                    Target.Membership -> page = Page.Membership
                                }
                            },
                            onAllActivity = { page = Page.Activity },
                            // The pencil of the card: straight to "Mon profil".
                            onEditProfile = {
                                membershipTab = MembershipTab.PROFILE
                                page = Page.Membership
                            },
                            refreshing = refreshing,
                            onRefresh = {
                                refreshing = true
                                resumed++
                            },
                        )
                    Page.Membership ->
                        membership?.let { read ->
                            MembershipScreen(
                                read,
                                shown,
                                membershipTab,
                                onTab = { membershipTab = it },
                                onBack = back,
                                // The payment page in the browser; back in the app, the next reading shows the new membership.
                                onRenew = {
                                    failure {
                                        val link = api.renewal()
                                        uri.openUri(link.url)
                                    }
                                },
                                actions =
                                    ProfileActions(
                                        onSave = { profile ->
                                            // The card changes once the server took it; until then the sheet stays open.
                                            failure { api.updateProfile(profile.jobTitle, profile.company, profile.city, profile.sector) }.also { said ->
                                                if (said == null) home = shown.copy(profile = profile, card = shown.card.with(profile))
                                            }
                                        },
                                        onEmail = { newEmail = it },
                                        onPhoto = { photo ->
                                            failure { api.uploadPhoto(photo) }.also { said ->
                                                if (said == null) {
                                                    // Read again from the server, turned and cut as it will stay.
                                                    data.forgetPhoto()
                                                    resumed++
                                                }
                                            }
                                        },
                                        lockOnOpen = lockOnOpen,
                                        onLockOnOpen = {
                                            lockOnOpen = it
                                            secure.put(LOCK_KEY, if (it) "1" else "0")
                                        },
                                        onChangePin = { changingPin = true },
                                        onLogout = {
                                            scope.launch { api.logout() }
                                            signedOut(null)
                                        },
                                    ),
                            )
                        } ?: Loading(failed = membershipFailed, onRetry = { membershipTry++ })
                    Page.Events -> EventsScreen(events, back) { event = it }
                    Page.Activity ->
                        ActivityScreen(activity, back) { row ->
                            when (val target = row.target) {
                                is Target.Event -> event = events.firstOrNull { it.id == target.id }
                                is Target.Receipt -> openReceipt(target.code)
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
                EventSheet(it, memberActive = shown.status is MemberStatus.Active, onDismiss = { event = null }, onRenew = {
                    event = null
                    page = Page.Membership
                })
            }
            receipt?.let {
                ModalBottomSheet({ receipt = null }, containerColor = MaterialTheme.colorScheme.surface) { ReceiptPanel(it) }
            }
        }
        newEmail?.let { email ->
            EmailChangeSheet(
                email,
                onPin = { pin -> failure { api.requestEmail(email, pin) } },
                onCode = { code -> failure { api.confirmEmail(code) } },
                onDone = {
                    newEmail = null
                    resumed++
                },
                onDismiss = {
                    newEmail = null
                    resumed++
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
                    failure { api.answerRequest(request.id, accept = true) }
                        .also { said -> if (said == null) signedOut("Ton compte est maintenant ouvert sur un autre téléphone.") }
                },
                onRefuse = { failure { api.answerRequest(request.id, accept = false) } },
                onDismiss = { deviceRequest = null },
            )
        }
    }
}

/** Null when the call went through; otherwise what to tell the member, the tries left for a wrong PIN. */
private suspend fun failure(call: suspend () -> Unit): String? = try {
    call()
    null
} catch (error: ApiException) {
    if (error.reason == "PIN_WRONG") wrongPin(error.triesLeft ?: 0) else error.message
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    // The network's own words are not the member's: always the same sentence.
    "Pas de réseau. Réessaie dans un instant."
}
