package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.launcher.QuickNotesPolicy
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.sin

@Composable
internal fun WorkQuickNotesTile(
    notes: List<QuickNote>,
    onAdd: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onUpdate: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editingNote by remember { mutableStateOf<QuickNote?>(null) }
    var creatingNote by remember { mutableStateOf(false) }
    CozyTile(
        label = "Notas rápidas",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        if (notes.isEmpty()) {
            TileBody("Captura una idea sin salir de WORK.")
        } else {
            notes.forEach { note ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Editar ${note.title}",
                        ) { editingNote = note }
                        .padding(vertical = 5.dp),
                ) {
                    BasicText(
                        text = if (note.type == QuickNoteType.CHECKLIST) "[ ]" else "·",
                        style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 11),
                        modifier = Modifier.padding(end = 7.dp),
                    )
                    BasicText(
                        text = note.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary),
                    )
                }
            }
        }
        if (notes.size < 3) TileAction("Añadir") { creatingNote = true }
    }

    if (creatingNote) {
        QuickNoteEditorDialog(
            note = null,
            onDismiss = { creatingNote = false },
            onSave = { title, type, body, checklist ->
                creatingNote = false
                onAdd(title, type, body, checklist)
            },
            onDelete = null,
        )
    }
    editingNote?.let { note ->
        QuickNoteEditorDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { title, type, body, checklist ->
                editingNote = null
                onUpdate(note.id, title, type, body, checklist)
            },
            onDelete = { editingNote = null; onDelete(note.id) },
        )
    }
}

@Composable
private fun QuickNoteEditorDialog(
    note: QuickNote?,
    onDismiss: () -> Unit,
    onSave: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var type by remember(note?.id) { mutableStateOf(note?.type ?: QuickNoteType.TEXT) }
    var body by remember(note?.id) { mutableStateOf(note?.body.orEmpty()) }
    var checklist by remember(note?.id) { mutableStateOf(note?.checklist.orEmpty()) }
    val validTitle = QuickNotesPolicy.sanitizeTitle(title)
    RofiDialog(
        title = if (note == null) "nueva nota" else "editar nota",
        onDismiss = onDismiss,
        actions = {
            if (onDelete != null) RofiAction("eliminar", onDelete, danger = true)
            Spacer(Modifier.weight(1f))
            RofiAction("cancelar", onDismiss)
            RofiAction(
                label = "guardar",
                enabled = validTitle != null,
                onClick = {
                    validTitle?.let { cleanTitle ->
                        onSave(cleanTitle, type, body, checklist)
                    }
                },
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            RofiEditorField(
                label = "title",
                value = title,
                hint = "visible en WORK · ${title.length}/${QuickNotesPolicy.MAX_TITLE_LENGTH}",
                singleLine = true,
                onValueChange = { value ->
                    title = value.replace('\n', ' ').replace('\r', ' ')
                        .take(QuickNotesPolicy.MAX_TITLE_LENGTH)
                },
            )
            RofiNoteTypeSelector(selected = type, onSelected = { type = it })
            when (type) {
                QuickNoteType.TEXT -> RofiEditorField(
                    label = "content",
                    value = body,
                    hint = "texto libre · ${body.length}/${QuickNotesPolicy.MAX_BODY_LENGTH}",
                    minHeight = 170.dp,
                    onValueChange = { body = it.take(QuickNotesPolicy.MAX_BODY_LENGTH) },
                )
                QuickNoteType.CHECKLIST -> {
                    checklist.forEach { item ->
                        RofiChecklistEditorRow(
                            item = item,
                            onCheckedChange = { checked ->
                                checklist = checklist.map { current ->
                                    if (current.id == item.id) current.copy(checked = checked)
                                    else current
                                }
                            },
                            onTextChange = { value ->
                                checklist = checklist.map { current ->
                                    if (current.id == item.id) current.copy(
                                        text = value.replace('\n', ' ').replace('\r', ' ')
                                            .take(QuickNotesPolicy.MAX_CHECKLIST_ITEM_LENGTH),
                                    ) else current
                                }
                            },
                            onDelete = {
                                checklist = checklist.filterNot { it.id == item.id }
                            },
                        )
                    }
                    if (checklist.size < QuickNotesPolicy.MAX_CHECKLIST_ITEMS) {
                        RofiAction(
                            label = "+ item",
                            onClick = {
                                val nextId =
                                    (checklist.maxOfOrNull(QuickNoteChecklistItem::id) ?: 0L) + 1L
                                checklist = checklist + QuickNoteChecklistItem(nextId, "")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RofiEditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean = false,
    minHeight: Dp = 46.dp,
) {
    val palette = LocalVeilPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BasicText("$label:", style = workspaceMonoStyle(palette.accentActive, 9))
            BasicText(hint, style = workspaceMonoStyle(palette.contentMuted, 8))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = workspaceMonoStyle(palette.contentPrimary, 11),
            cursorBrush = SolidColor(palette.accentActive),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.fieldBackground.copy(alpha = 0.72f))
                .border(1.dp, palette.divider, RoundedCornerShape(3.dp))
                .padding(horizontal = 11.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun RofiNoteTypeSelector(
    selected: QuickNoteType,
    onSelected: (QuickNoteType) -> Unit,
) {
    val palette = LocalVeilPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BasicText("mode:", style = workspaceMonoStyle(palette.accentActive, 9))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(
                QuickNoteType.TEXT to "texto",
                QuickNoteType.CHECKLIST to "checklist",
            ).forEach { (type, label) ->
                val active = selected == type
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) palette.accentActive.copy(alpha = 0.15f)
                            else palette.fieldBackground.copy(alpha = 0.56f),
                        )
                        .border(
                            1.dp,
                            if (active) palette.accentActive else palette.divider,
                            RoundedCornerShape(3.dp),
                        )
                        .clickable(role = Role.RadioButton) { onSelected(type) }
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                ) {
                    BasicText(
                        if (active) ">" else " ",
                        style = workspaceMonoStyle(palette.accentActive, 10),
                    )
                    BasicText(
                        label,
                        style = workspaceMonoStyle(
                            if (active) palette.contentPrimary else palette.contentSecondary,
                            10,
                        ),
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RofiChecklistEditorRow(
    item: QuickNoteChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clickable(role = Role.Checkbox) { onCheckedChange(!item.checked) },
        ) {
            BasicText(
                if (item.checked) "[x]" else "[ ]",
                style = workspaceMonoStyle(
                    if (item.checked) palette.accentActive else palette.contentSecondary,
                    11,
                ),
            )
        }
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            singleLine = true,
            textStyle = workspaceMonoStyle(palette.contentPrimary, 10),
            cursorBrush = SolidColor(palette.accentActive),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.fieldBackground.copy(alpha = 0.72f))
                .border(1.dp, palette.divider, RoundedCornerShape(3.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        )
        RofiAction("x", onDelete, danger = true)
    }
}

