@file:OptIn(ExperimentalMaterial3Api::class)

package app.ranking.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ranking.RankingViewModel
import app.ranking.data.Criterion
import kotlinx.coroutines.flow.flowOf

@Composable
fun ItemScreen(vm: RankingViewModel, itemId: Long, onBack: () -> Unit) {
    val current by remember(itemId) { vm.item(itemId) }.collectAsState(initial = null)
    val scores by remember(itemId) { vm.itemScores(itemId) }.collectAsState(initial = emptyList())
    val comments by remember(itemId) { vm.comments(itemId) }.collectAsState(initial = emptyList())

    val listId = current?.listId
    val criteria by remember(listId) {
        if (listId != null) vm.criteria(listId) else flowOf(emptyList<Criterion>())
    }.collectAsState(initial = emptyList())

    val scoreMap = remember(scores) { scores.associate { it.criterionId to it.value } }
    val overall = remember(scores) {
        if (scores.isEmpty()) null else scores.map { it.value }.average().toFloat()
    }

    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setPhoto(itemId, uri) }
    val pickPhoto = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Renomear")
                    }
                    IconButton(onClick = { deleting = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir item")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val photo = current?.photoPath
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PhotoBox(
                        photoPath = photo,
                        placeholder = "📷  Toque para adicionar uma foto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clickable { pickPhoto() }
                    )
                    Row {
                        TextButton(onClick = { pickPhoto() }) {
                            Text(if (photo == null) "Adicionar foto" else "Trocar foto")
                        }
                        if (photo != null) {
                            TextButton(onClick = { vm.removePhoto(itemId) }) {
                                Text("Remover foto")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nota geral (média)", style = MaterialTheme.typography.labelLarge)
                        Text(
                            formatScore(overall),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Text("Avaliação", style = MaterialTheme.typography.titleLarge) }

            items(criteria, key = { it.id }) { c ->
                CriterionRow(
                    name = c.name,
                    value = scoreMap[c.id],
                    onSave = { v -> vm.setScore(itemId, c.id, v) },
                    onClear = { vm.clearScore(itemId, c.id) }
                )
            }

            item { Text("Comentários", style = MaterialTheme.typography.titleLarge) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Novo comentário") }
                    )
                    IconButton(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            vm.addComment(itemId, draft)
                            draft = ""
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar comentário")
                    }
                }
            }

            if (comments.isEmpty()) {
                item {
                    Text(
                        "Nenhum comentário ainda. Anote o que achou, defeitos, em que situação testou…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(comments, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.text)
                                Text(
                                    formatDate(c.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.deleteComment(c.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Excluir comentário")
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        TextInputDialog(
            title = "Renomear item",
            label = "Nome",
            initial = current?.name ?: "",
            onConfirm = { vm.renameItem(itemId, it) },
            onDismiss = { renaming = false }
        )
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Excluir este item?") },
            text = { Text("As notas e os comentários dele também serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    vm.deleteItem(itemId)
                    onBack()
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CriterionRow(
    name: String,
    value: Float?,
    onSave: (Float) -> Unit,
    onClear: () -> Unit
) {
    // Valor local para o slider ficar fluido; só grava no banco ao soltar.
    var local by remember(value) { mutableFloatStateOf(value ?: 5f) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (value != null) {
                Text(
                    formatScore(local),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar nota de $name")
                }
            }
        }
        if (value == null) {
            OutlinedButton(onClick = { onSave(5f) }) { Text("Avaliar") }
        } else {
            Slider(
                value = local,
                onValueChange = { local = it },
                onValueChangeFinished = { onSave(local) },
                valueRange = 0f..10f,
                steps = 19
            )
        }
    }
}
