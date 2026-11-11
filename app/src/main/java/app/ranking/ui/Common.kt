package app.ranking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.ranking.data.Score
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/** Nota com 1 casa decimal e vírgula, ou "—" se ainda não houver nota. */
fun formatScore(value: Float?): String =
    if (value == null) "—" else String.format(PT_BR, "%.1f", value)

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", PT_BR).format(Date(millis))

/** Média das notas de cada item (só dos critérios que já foram avaliados). */
fun averages(scores: List<Score>): Map<Long, Float> =
    scores.groupBy { it.itemId }
        .mapValues { (_, list) -> list.map { it.value }.average().toFloat() }

val LIST_EMOJIS = listOf("🎬", "📺", "🎮", "📚", "🎧", "🛒", "🍔", "☕", "🍷", "📱", "💻", "👟", "⭐")

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "Salvar",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onConfirm(text)
                    onDismiss()
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Foto do item (recortada para preencher) ou um espaço reservado quando não há foto. */
@Composable
fun PhotoBox(photoPath: String?, modifier: Modifier = Modifier, placeholder: String = "📷") {
    val shape = RoundedCornerShape(12.dp)
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
