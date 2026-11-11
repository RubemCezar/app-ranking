@file:OptIn(ExperimentalMaterial3Api::class)

package app.ranking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ranking.RankingViewModel

private const val MAX_COMMENTS_SHOWN = 5

@Composable
fun CompareScreen(vm: RankingViewModel, listId: Long, idsParam: String, onBack: () -> Unit) {
    val ids = remember(idsParam) { idsParam.split(",").mapNotNull { it.toLongOrNull() } }

    val allItems by remember(listId) { vm.items(listId) }.collectAsState(initial = emptyList())
    val criteria by remember(listId) { vm.criteria(listId) }.collectAsState(initial = emptyList())
    val scores by remember(listId) { vm.scores(listId) }.collectAsState(initial = emptyList())
    val comments by remember(idsParam) { vm.commentsFor(ids) }.collectAsState(initial = emptyList())

    // Mantém a ordem em que o usuário escolheu os itens.
    val chosen = remember(allItems, ids) {
        ids.mapNotNull { id -> allItems.firstOrNull { it.id == id } }
    }
    val byKey = remember(scores) {
        scores.associate { (it.itemId to it.criterionId) to it.value }
    }
    val avg = remember(scores) { averages(scores) }

    // Em quantos critérios cada item vence sozinho (empate não conta).
    val wins = remember(chosen, criteria, byKey) {
        val result = mutableMapOf<Long, Int>()
        criteria.forEach { c ->
            val values = chosen.mapNotNull { i -> byKey[i.id to c.id]?.let { v -> i.id to v } }
            if (values.size >= 2) {
                val best = values.maxOf { it.second }
                val top = values.filter { it.second == best }
                if (top.size == 1) {
                    val winner = top[0].first
                    result[winner] = (result[winner] ?: 0) + 1
                }
            }
        }
        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparação") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho: nome de cada item e quantos critérios ele vence.
            item {
                Row(Modifier.fillMaxWidth()) {
                    chosen.forEach { i ->
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PhotoBox(
                                photoPath = i.photoPath,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                i.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "vence em ${wins[i.id] ?: 0} de ${criteria.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                CompareRow(label = "Nota geral (média)", values = chosen.map { avg[it.id] })
            }

            criteria.forEach { c ->
                item(key = "criterion-${c.id}") {
                    CompareRow(label = c.name, values = chosen.map { byKey[it.id to c.id] })
                }
            }

            // Comentários de cada item, lado a lado em sequência.
            chosen.forEach { i ->
                item(key = "comments-${i.id}") {
                    val itemComments = comments.filter { it.itemId == i.id }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Comentários — ${i.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (itemComments.isEmpty()) {
                                Text(
                                    "Sem comentários.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                itemComments.take(MAX_COMMENTS_SHOWN).forEach { c ->
                                    Column {
                                        Text(c.text)
                                        Text(
                                            formatDate(c.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (itemComments.size > MAX_COMMENTS_SHOWN) {
                                    Text(
                                        "+ ${itemComments.size - MAX_COMMENTS_SHOWN} comentários mais antigos",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Uma linha da comparação: rótulo + uma célula por item, destacando a maior nota. */
@Composable
private fun CompareRow(label: String, values: List<Float?>) {
    val scored = values.filterNotNull()
    val best = scored.maxOrNull()
    val canHighlight = scored.size >= 2 && best != null

    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        Row(Modifier.fillMaxWidth()) {
            values.forEach { v ->
                ScoreCell(
                    value = v,
                    highlight = canHighlight && v != null && v == best,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScoreCell(value: Float?, highlight: Boolean, modifier: Modifier = Modifier) {
    val container = if (highlight) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (highlight) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(container, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            formatScore(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = content
        )
    }
}
