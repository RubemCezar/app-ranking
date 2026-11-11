package app.ranking

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.ranking.data.AppDatabase
import app.ranking.data.Comment
import app.ranking.data.Criterion
import app.ranking.data.Item
import app.ranking.data.RankList
import app.ranking.data.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RankingViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.dao()

    // ---- Leitura (Flows) ----
    val lists = dao.observeLists()
    fun list(id: Long) = dao.observeList(id)
    fun criteria(listId: Long) = dao.observeCriteria(listId)
    fun items(listId: Long) = dao.observeItems(listId)
    fun scores(listId: Long) = dao.observeScoresForList(listId)
    fun item(id: Long) = dao.observeItem(id)
    fun itemScores(itemId: Long) = dao.observeScoresForItem(itemId)
    fun comments(itemId: Long) = dao.observeComments(itemId)
    fun commentsFor(itemIds: List<Long>) = dao.observeCommentsFor(itemIds)

    suspend fun criteriaNow(listId: Long): List<Criterion> = dao.getCriteria(listId)

    // ---- Listas e critérios ----
    fun saveList(id: Long?, name: String, emoji: String, criteriaNames: List<String>) {
        viewModelScope.launch {
            val names = criteriaNames
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .ifEmpty { listOf("Nota geral") }
            val cleanName = name.trim()
            val cleanEmoji = emoji.ifBlank { "⭐" }

            db.withTransaction {
                val listId: Long
                if (id == null) {
                    listId = dao.insertList(RankList(name = cleanName, emoji = cleanEmoji))
                } else {
                    dao.updateList(RankList(id = id, name = cleanName, emoji = cleanEmoji))
                    listId = id
                }

                val existing = dao.getCriteria(listId)
                // Remove critérios que saíram (as notas deles somem junto, via CASCADE).
                existing
                    .filter { e -> names.none { it.equals(e.name, ignoreCase = true) } }
                    .forEach { dao.deleteCriterion(it.id) }

                names.forEachIndexed { index, n ->
                    val match = existing.firstOrNull { it.name.equals(n, ignoreCase = true) }
                    if (match == null) {
                        dao.insertCriterion(Criterion(listId = listId, name = n, position = index))
                    } else if (match.position != index || match.name != n) {
                        dao.updateCriterion(match.copy(name = n, position = index))
                    }
                }
            }
        }
    }

    fun deleteList(id: Long) {
        viewModelScope.launch {
            val photos = dao.getPhotoPaths(id)
            dao.deleteList(id)
            removeFiles(photos)
        }
    }

    // ---- Itens ----
    fun addItem(listId: Long, name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = dao.insertItem(
                Item(listId = listId, name = name.trim(), createdAt = System.currentTimeMillis())
            )
            onCreated(id)
        }
    }

    fun renameItem(id: Long, name: String) {
        viewModelScope.launch { dao.renameItem(id, name.trim()) }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            val photo = dao.getItem(id)?.photoPath
            dao.deleteItem(id)
            photo?.let { removeFiles(listOf(it)) }
        }
    }

    // ---- Foto do item ----
    /** Copia a foto escolhida na galeria para o armazenamento privado do app. */
    fun setPhoto(itemId: Long, uri: Uri) {
        viewModelScope.launch {
            val newPath = withContext(Dispatchers.IO) { copyToPrivateStorage(uri) } ?: return@launch
            val oldPath = dao.getItem(itemId)?.photoPath
            dao.updatePhoto(itemId, newPath)
            oldPath?.let { removeFiles(listOf(it)) }
        }
    }

    fun removePhoto(itemId: Long) {
        viewModelScope.launch {
            val oldPath = dao.getItem(itemId)?.photoPath
            dao.updatePhoto(itemId, null)
            oldPath?.let { removeFiles(listOf(it)) }
        }
    }

    private fun copyToPrivateStorage(uri: Uri): String? {
        return try {
            val context = getApplication<Application>()
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val file = File(dir, "photo_${System.currentTimeMillis()}.img")
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { source -> file.outputStream().use { out -> source.copyTo(out) } }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun removeFiles(paths: List<String>) {
        withContext(Dispatchers.IO) {
            paths.forEach { runCatching { File(it).delete() } }
        }
    }

    // ---- Notas ----
    fun setScore(itemId: Long, criterionId: Long, value: Float) {
        viewModelScope.launch { dao.upsertScore(Score(itemId, criterionId, value)) }
    }

    fun clearScore(itemId: Long, criterionId: Long) {
        viewModelScope.launch { dao.clearScore(itemId, criterionId) }
    }

    // ---- Comentários ----
    fun addComment(itemId: Long, text: String) {
        viewModelScope.launch {
            dao.insertComment(
                Comment(itemId = itemId, text = text.trim(), createdAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteComment(id: Long) {
        viewModelScope.launch { dao.deleteComment(id) }
    }
}
