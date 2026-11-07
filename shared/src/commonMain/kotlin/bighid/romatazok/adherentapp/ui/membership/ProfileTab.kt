package bighid.romatazok.adherentapp.ui.membership

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.Hair
import bighid.romatazok.adherentapp.ui.Loader
import bighid.romatazok.adherentapp.ui.SoftRow
import bighid.romatazok.adherentapp.ui.StateDot
import bighid.romatazok.adherentapp.ui.home.Profile
import kotlinx.coroutines.launch

/** One line of the profile: what it shows, and how a new value goes in. */
private enum class Field(
    val label: String,
) {
    JOB("Profession"),
    COMPANY("Entreprise"),
    CITY("Ville"),
    SECTOR("Secteur"),
    EMAIL("Email"),
    ;

    fun of(profile: Profile): String? =
        when (this) {
            JOB -> profile.jobTitle
            COMPANY -> profile.company
            CITY -> profile.city
            SECTOR -> profile.sector
            EMAIL -> profile.email
        }

    fun set(
        profile: Profile,
        value: String?,
    ): Profile =
        when (this) {
            JOB -> profile.copy(jobTitle = value)
            COMPANY -> profile.copy(company = value)
            CITY -> profile.copy(city = value)
            SECTOR -> profile.copy(sector = value)
            EMAIL -> profile
        }
}

/** Every line is touched and changes alone. The name and the number are the club's: they are at the top, read-only. */
@Composable
internal fun ProfileTab(
    profile: Profile,
    actions: ProfileActions,
) {
    var editing by remember { mutableStateOf<Field?>(null) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)) {
            itemsIndexed(Field.entries, key = { _, it -> it.name }) { position, field ->
                if (position > 0) Hair()
                SoftRow(field.label, value = field.of(profile) ?: "Ajouter", onClick = { editing = field })
            }
            item {
                Spacer(Modifier.height(18.dp))
                SoftRow("Changer mon code PIN", onClick = actions.onChangePin)
                Hair()
                SoftRow(
                    "PIN à l'ouverture",
                    trailing = {
                        Switch(actions.lockOnOpen, actions.onLockOnOpen, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                    },
                )
            }
        }
        Text(
            "Se déconnecter",
            Modifier.fillMaxWidth().clickable(onClick = actions.onLogout).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom + 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
    when (val field = editing) {
        null -> Unit
        Field.SECTOR ->
            SectorSheet(field.of(profile), onDismiss = { editing = null }) { sector ->
                actions.onSave(Field.SECTOR.set(profile, sector)).also { if (it == null) editing = null }
            }
        else ->
            FieldSheet(field, field.of(profile).orEmpty(), onDismiss = { editing = null }) { typed ->
                val value = typed.trim().ifEmpty { null }
                when {
                    field != Field.EMAIL -> actions.onSave(field.set(profile, value)).also { if (it == null) editing = null }
                    else -> {
                        editing = null
                        // Changed only once the PIN then its code came back.
                        if (value != null && value != profile.email) actions.onEmail(value)
                        null
                    }
                }
            }
    }
}

/** One field, the keyboard already open, and it is done. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldSheet(
    field: Field,
    current: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> String?,
) {
    var text by remember { mutableStateOf(current) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().imePadding().padding(start = 22.dp, end = 22.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(field.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            BasicTextField(
                text,
                {
                    text = it
                    error = null
                },
                Modifier.fillMaxWidth().focusRequester(focus),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = if (field == Field.EMAIL) KeyboardType.Email else KeyboardType.Text),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 15.dp)) { inner() }
                },
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = !saving) {
                        saving = true
                        scope.launch {
                            error = onSave(text)
                            saving = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (saving) Loader(size = 22.dp, onAccent = true) else Text("Enregistrer", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** The sectors of the club: a tap chooses and saves. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectorSheet(
    current: String?,
    onDismiss: () -> Unit,
    onChoose: suspend (String) -> String?,
) {
    var saving by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        error?.let { Text(it, Modifier.padding(horizontal = 22.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 26.dp)) {
            itemsIndexed(SECTORS, key = { _, it -> it }) { position, sector ->
                if (position > 0) Hair()
                SoftRow(
                    sector,
                    trailing = {
                        when {
                            saving == sector -> Loader(size = 18.dp)
                            sector == current -> StateDot(MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        if (saving == null) {
                            saving = sector
                            error = null
                            scope.launch {
                                error = onChoose(sector)
                                saving = null
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Until the app reads them from the server: the sectors seeded in the database. */
private val SECTORS =
    listOf(
        "Agriculture et agroalimentaire", "Architecture et urbanisme", "Art, culture et événementiel", "Banque et assurance",
        "BTP et immobilier", "Commerce et distribution", "Communication, marketing et médias", "Conseil et audit",
        "Diplomatie et organisations internationales", "Éducation et formation", "Énergie, mines et pétrole",
        "Entrepreneuriat social et ONG", "Finance et comptabilité", "Hôtellerie, restauration et tourisme", "Industrie",
        "Informatique et numérique", "Juridique", "Logistique et transport", "Mode, beauté et bien-être",
        "Ressources humaines", "Santé et pharmacie", "Secteur public et administration", "Sport", "Télécommunications", "Autre",
    )
