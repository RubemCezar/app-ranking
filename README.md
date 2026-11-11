# App Ranking

App Android para classificar coisas (filmes, produtos testados, jogos…), com nota por critérios, comentários e comparação lado a lado.

## Como abrir e rodar

1. Instale o **Android Studio** (versão recente).
2. `File > Open` e escolha esta pasta (`AppRanking`). Aguarde o *Gradle Sync*.
3. Conecte o celular (depuração USB ligada) ou crie um emulador e clique em **Run**.

Se o Android Studio sugerir atualizar versões do AGP/Kotlin/Compose, pode aceitar.

## Stack

Kotlin + Jetpack Compose + Material 3 + Room (tudo local, offline, sem backend).

## O que o app faz

- **Listas**: cada lista tem nome, emoji e seus próprios critérios (ex.: Qualidade, Preço, Durabilidade). Sem critérios, usa só "Nota geral".
- **Itens**: nota de 0 a 10 (passos de 0,5) por critério; a nota geral é a média dos critérios avaliados.
- **Foto**: você escolhe a foto do produto na galeria; ela fica guardada dentro do app e aparece no item, no ranking (miniatura) e na comparação.
- **Ranking**: itens ordenados pela nota geral; os ainda sem nota ficam no fim.
- **Comentários**: vários por item, com data e hora.
- **Comparação**: escolha 2 ou 3 itens ("Comparar" no topo do ranking) e veja nota por critério lado a lado, com a maior destacada, quantos critérios cada um vence e os comentários.

## Estrutura

- `data/Database.kt` – entidades, DAO e banco Room
- `RankingViewModel.kt` – acesso aos dados
- `ui/` – telas: `ListsScreen`, `RankingScreen`, `ItemScreen`, `CompareScreen`
- `MainActivity.kt` – navegação

## APK automático

A cada push na branch `main`, o GitHub Actions compila o app. Para baixar o APK: aba **Actions** > último build > **Artifacts** > `app-ranking-debug-apk`.
