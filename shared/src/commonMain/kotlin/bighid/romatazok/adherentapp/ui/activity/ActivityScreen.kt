package bighid.romatazok.adherentapp.ui.activity

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_close
import adherentapp.shared.generated.resources.ic_search
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.PageBar
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import bighid.romatazok.adherentapp.ui.home.ActivityLine
import bighid.romatazok.adherentapp.ui.home.ActivityRow
import bighid.romatazok.adherentapp.ui.theme.Brand
import org.jetbrains.compose.resources.painterResource

/**
 * The whole history, most recent first, searched and filtered together. A row opens what it is about in a bottom
 * sheet: nothing goes deeper than this page.
 */
@Composable
fun ActivityScreen(
    rows: List<ActivityRow>,
    onBack: () -> Unit,
    onOpen: (ActivityRow) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ActivityFilter.ALL) }
    // The history comes by pages, the next one when the bottom of the list is reached.
    var shown by remember { mutableStateOf(PAGE) }
    val list = rememberLazyListState()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val words = query.normalized().split(" ").filter { it.isNotEmpty() }
    val found = rows.filter { row -> filter.keeps(row.kind) && words.all { it in row.searchText() } }
    val visible = found.take(shown)
    val atEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 2 } }
    LaunchedEffect(atEnd, found.size) {
        if (atEnd && shown < found.size) shown += PAGE
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageBar("Activité", onBack)
        SearchField(query, Modifier.padding(horizontal = 16.dp)) {
            query = it
            shown = PAGE
        }
        Segmented(filter, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)) {
            filter = it
            shown = PAGE
        }
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = bottom + 16.dp)) {
            if (found.isEmpty()) {
                item { Empty(rows.isEmpty(), query) { query = "" } }
            }
            items(visible, key = { it.id }) { row ->
                ActivityLine(row) { onOpen(row) }
                Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(Res.drawable.ic_search), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Rechercher", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            BasicTextField(
                query,
                onChange,
                Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(Brand.Orange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // The list filters while typing: the search key only puts the keyboard away.
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
        }
        if (query.isNotEmpty()) {
            Box(Modifier.size(36.dp).clip(CircleShape).clickable { onChange("") }, contentAlignment = Alignment.Center) {
                Icon(painterResource(Res.drawable.ic_close), "Effacer la recherche", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The same soft segmented filter as the membership, the selection gliding from one choice to the other. */
@Composable
private fun Segmented(
    filter: ActivityFilter,
    modifier: Modifier,
    onFilter: (ActivityFilter) -> Unit,
) {
    val choices = ActivityFilter.entries
    BoxWithConstraints(modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
        val width = maxWidth / choices.size
        val x by animateDpAsState(width * filter.ordinal)
        Box(Modifier.offset(x = x).width(width).fillMaxHeight().clip(RoundedCornerShape(50)).background(NAVY_LIGHT))
        Row(Modifier.fillMaxSize()) {
            choices.forEach { choice ->
                val color by animateColorAsState(if (choice == filter) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).clickable { onFilter(choice) }, contentAlignment = Alignment.Center) {
                    Text(choice.label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Empty(
    nothingYet: Boolean,
    query: String,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val text =
            when {
                nothingYet -> "Ton activité apparaîtra ici."
                query.isNotBlank() -> "Aucun résultat pour « ${query.trim()} »."
                else -> "Rien ici pour le moment."
            }
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.Center)
        if (!nothingYet && query.isNotBlank()) {
            Text(
                "Effacer la recherche",
                Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClear).padding(horizontal = 18.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

/** What the search looks into: the title, the time and the amount, without accents nor spaces in the numbers. */
private fun ActivityRow.searchText() = "$title $time ${amount.orEmpty()} ${amount.orEmpty().replace(" ", "")}".normalized()

/** Lower case, without accents, so that "diner" finds "Dîner". */
private fun String.normalized(): String =
    lowercase()
        .map { c -> ACCENTS.entries.firstOrNull { c in it.key }?.value ?: c }
        .joinToString("")
        .replace(' ', ' ')
        .trim()

private val ACCENTS =
    mapOf(
        "àâä" to 'a',
        "éèêë" to 'e',
        "îï" to 'i',
        "ôö" to 'o',
        "ùûü" to 'u',
        "ç" to 'c',
    )

private const val PAGE = 10
