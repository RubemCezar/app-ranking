package app.ranking.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ranking.RankingViewModel
import app.ranking.update.UpdateState

/** Faixa na parte de baixo da tela inicial quando existe uma versão nova no GitHub. */
@Composable
fun UpdateBanner(vm: RankingViewModel) {
    val state by vm.updateState.collectAsState()

    val title: String
    val buttonLabel: String?
    val onClick: () -> Unit
    when (val s = state) {
        is UpdateState.None -> return
        is UpdateState.Available -> {
            title = "Nova versão disponível (v${s.versionCode})"
            buttonLabel = "Baixar"
            onClick = vm::downloadUpdate
        }
        is UpdateState.Downloading -> {
            title = "Baixando a atualização…"
            buttonLabel = null
            onClick = {}
        }
        is UpdateState.Ready -> {
            title = "Atualização v${s.versionCode} pronta"
            buttonLabel = "Instalar"
            onClick = vm::installUpdate
        }
        is UpdateState.Failed -> {
            title = "Não consegui baixar a atualização"
            buttonLabel = "Tentar de novo"
            onClick = vm::downloadUpdate
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (buttonLabel != null) {
                    Button(onClick = onClick) { Text(buttonLabel) }
                }
            }
            if (state is UpdateState.Downloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
