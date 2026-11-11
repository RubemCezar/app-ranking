@file:OptIn(ExperimentalMaterial3Api::class)

package app.ranking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ranking.RankingViewModel
import app.ranking.data.RankList

@Composable
fun ListsScreen(vm: RankingViewModel, onOpen: (Long) -> Unit) {
    val lists by vm.lists.collectAsState(initial = emptyList())
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RankList?>(null) }
    var deleting by remember { mutableStateOf<RankList?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Meus rankings") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova lista") }
            )
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Crie sua primeira lista, como \"Filmes\" ou \"Fones de ouvido\", e comece a avaliar.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lists, key = { it.id }) { list ->
                    var menu by remember { mutableStateOf(false) }
                    Card(
                        onClick = { onOpen(list.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(list.emoji, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                list.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            )
                            Box {
                                IconButton(onClick = { menu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                                }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Editar") },
                                        onClick = {
                                            menu = false
                                            editing = list
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir") },
                                        onClick = {
                                            menu = false
                                            deleting = list
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        ListEditDialog(vm = vm, list = null, onDismiss = { creating = false })
    }
    editing?.let { target ->
        ListEditDialog(vm = vm, list = target, onDismiss = { editing = null })
    }
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir \"${target.name}\"?") },
            text = { Text("Todos os itens, notas e comentários dessa lista serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteList(target.id)
                    deleting = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ListEditDialog(vm: RankingViewModel, list: RankList?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(list?.name ?: "") }
    var emoji by remember { mutableStateOf(list?.emoji ?: LIST_EMOJIS.first()) }
    var criteria by remember { mutableStateOf("") }
    // Ao editar, só libera o "Salvar" depois de carregar os critérios atuais.
    var loaded by remember { mutableStateOf(list == null) }

    LaunchedEffect(list?.id) {
        if (list != null) {
            criteria = vm.criteriaNow(list.id).joinToString(", ") { it.name }
            loaded = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (list == null) "Nova lista" else "Editar lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LIST_EMOJIS) { e ->
                        FilterChip(
                            selected = e == emoji,
                            onClick = { emoji = e },
                            label = { Text(e) }
                        )
                    }
                }
                OutlinedTextField(
                    value = criteria,
                    onValueChange = { criteria = it },
                    label = { Text("Critérios (separe por vírgula)") },
                    placeholder = { Text("Qualidade, Preço, Durabilidade") },
                    supportingText = { Text("Vazio = só \"Nota geral\".") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && loaded,
                onClick = {
                    vm.saveList(list?.id, name, emoji, criteria.split(","))
                    onDismiss()
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
