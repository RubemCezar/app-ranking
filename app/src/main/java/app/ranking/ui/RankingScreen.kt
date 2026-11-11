@file:OptIn(ExperimentalMaterial3Api::class)

package app.ranking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ranking.RankingViewModel
import app.ranking.data.Item

private const val MAX_COMPARE = 3

@Composable
fun RankingScreen(
    vm: RankingViewModel,
    listId: Long,
    onBack: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onCompare: (List<Long>) -> Unit
) {
    val list by remember(listId) { vm.list(listId) }.collectAsState(initial = null)
    val items by remember(listId) { vm.items(listId) }.collectAsState(initial = emptyList())
    val scores by remember(listId) { vm.scores(listId) }.collectAsState(initial = emptyList())

    val avg = remember(scores) { averages(scores) }
    val sorted = remember(items, avg) {
        items.sortedWith(
            compareByDescending<Item> { avg[it.id] != null }
                .thenByDescending { avg[it.id] ?: 0f }
                .thenBy { it.name.lowercase() }
        )
    }

    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.let { "${it.emoji} ${it.name}" } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (sorted.size >= 2) {
                        TextButton(onClick = {
                            selecting = !selecting
                            selected.clear()
                        }) { Text(if (selecting) "Cancelar" else "Comparar") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selecting) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (selected.size >= 2) {
                            val ids = selected.toList()
                            selecting = false
                            selected.clear()
                            onCompare(ids)
                        }
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = {
                        Text(
                            if (selected.size < 2) "Escolha 2 ou $MAX_COMPARE itens"
                            else "Comparar ${selected.size}"
                        )
                    }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { adding = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Novo item") }
                )
            }
        }
    ) { padding ->
        if (sorted.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nenhum item ainda. Toque em \"Novo item\" para adicionar o primeiro.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                itemsIndexed(sorted, key = { _, row -> row.id }) { index, row ->
                    val checked = row.id in selected
                    val score = avg[row.id]
                    ListItem(
                        modifier = Modifier.clickable {
                            if (selecting) {
                                if (checked) {
                                    selected.remove(row.id)
                                } else if (selected.size < MAX_COMPARE) {
                                    selected.add(row.id)
                                }
                            } else {
                                onOpenItem(row.id)
                            }
                        },
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selecting) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                } else {
                                    Text(
                                        if (score != null) "${index + 1}º" else "–",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.width(32.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                PhotoBox(photoPath = row.photoPath, modifier = Modifier.size(52.dp))
                            }
                        },
                        headlineContent = { Text(row.name) },
                        trailingContent = {
                            Text(
                                formatScore(score),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = "Novo item",
            label = "Nome",
            confirmLabel = "Adicionar",
            onConfirm = { name -> vm.addItem(listId, name) { newId -> onOpenItem(newId) } },
            onDismiss = { adding = false }
        )
    }
}
