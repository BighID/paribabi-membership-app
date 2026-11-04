package bighid.romatazok.adherentapp.ui.home

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_chevron_right
import adherentapp.shared.generated.resources.ic_edit
import adherentapp.shared.generated.resources.ic_person
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.rememberPhotoPicker
import bighid.romatazok.adherentapp.ui.theme.Brand
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * Opened by the pencil: everything the card shows that the member may complete, the photo first. Opened in full and
 * scrolled, so that the keyboard never hides a field. The name and the number are the club's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    profile: Profile,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit,
    /** "Demander mon code PIN à l'ouverture", off unless the member turns it on. */
    lockOnOpen: Boolean,
    onLockOnOpen: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onLogout: () -> Unit,
) {
    var draft by remember { mutableStateOf(profile) }
    val picker = rememberPhotoPicker { bytes -> runCatching { bytes.decodeToImageBitmap() }.onSuccess { draft = draft.copy(photo = it) } }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Mon profil", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Photo(draft.photo, picker)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.fullName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(profile.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Nom et numéro : à changer auprès de l'équipe.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Field("Profession", draft.jobTitle) { draft = draft.copy(jobTitle = it) }
            Field("Entreprise", draft.company) { draft = draft.copy(company = it) }
            Field("Ville", draft.city) { draft = draft.copy(city = it) }
            SectorField(draft.sector) { draft = draft.copy(sector = it) }
            Field("Email", draft.email, KeyboardType.Email) { draft = draft.copy(email = it) }
            Spacer(Modifier.height(4.dp))
            Button(
                { onSave(draft.trimmed()) },
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("Enregistrer", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            Text("Sécurité", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.background)) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onChangePin).padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Changer mon code PIN", Modifier.weight(1f), fontSize = 15.sp)
                    Icon(painterResource(Res.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
                Row(
                    Modifier.fillMaxWidth().clickable { onLockOnOpen(!lockOnOpen) }.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Demander mon code PIN à l'ouverture", fontSize = 15.sp)
                        Text("Ou ton empreinte, ou ton visage, si ton téléphone le permet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(lockOnOpen, onLockOnOpen, colors = SwitchDefaults.colors(checkedTrackColor = Brand.Orange))
                }
            }
            TextButton(onLogout, Modifier.fillMaxWidth()) { Text("Se déconnecter", color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** The photo as on the card, portrait; a tap opens the gallery. */
@Composable
private fun Photo(
    photo: ImageBitmap?,
    onPick: (() -> Unit)?,
) {
    Box(Modifier.size(width = 84.dp, height = 104.dp)) {
        Box(
            Modifier
                .size(width = 84.dp, height = 104.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brand.BlueDark)
                .clickable(enabled = onPick != null) { onPick?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, "Ma photo", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(painterResource(Res.drawable.ic_person), null, Modifier.size(44.dp), tint = Color.White.copy(alpha = 0.55f))
            }
        }
        if (onPick != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(4.dp).size(28.dp).clip(CircleShape).background(Brand.Orange).clickable(onClick = onPick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(Res.drawable.ic_edit), if (photo == null) "Ajouter une photo" else "Changer la photo", Modifier.size(15.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String?,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value.orEmpty(),
        onChange,
        Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

/** One of the sectors of the club, the same list as the application form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectorField(
    value: String?,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }) {
        OutlinedTextField(
            value.orEmpty(),
            {},
            Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text("Secteur d'activité") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            shape = RoundedCornerShape(14.dp),
        )
        ExposedDropdownMenu(open, { open = false }) {
            SECTORS.forEach { sector ->
                DropdownMenuItem({ Text(sector) }, {
                    onChange(sector)
                    open = false
                })
            }
        }
    }
}

private fun Profile.trimmed() =
    copy(
        jobTitle = jobTitle?.trim()?.ifEmpty { null },
        company = company?.trim()?.ifEmpty { null },
        city = city?.trim()?.ifEmpty { null },
        email = email?.trim()?.ifEmpty { null },
    )

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
