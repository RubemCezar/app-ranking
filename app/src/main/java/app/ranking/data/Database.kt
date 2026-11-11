package app.ranking.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Uma lista de coisas a classificar, ex.: "Filmes" ou "Fones de ouvido". */
@Entity(tableName = "lists")
data class RankList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String
)

/** Um critério de avaliação de uma lista, ex.: "Qualidade", "Preço". */
@Entity(
    tableName = "criteria",
    foreignKeys = [
        ForeignKey(
            entity = RankList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class Criterion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val position: Int
)

/** Um item da lista, ex.: "Interestelar" ou "Fone X". */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = RankList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val createdAt: Long,
    val photoPath: String? = null
)

/** Nota (0 a 10) de um item em um critério. */
@Entity(
    tableName = "scores",
    primaryKeys = ["itemId", "criterionId"],
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Criterion::class,
            parentColumns = ["id"],
            childColumns = ["criterionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("criterionId")]
)
data class Score(
    val itemId: Long,
    val criterionId: Long,
    val value: Float
)

/** Comentário de um item (vários por item, com data). */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val text: String,
    val createdAt: Long
)

@Dao
interface RankDao {
    // Listas
    @Query("SELECT * FROM lists ORDER BY id DESC")
    fun observeLists(): Flow<List<RankList>>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observeList(id: Long): Flow<RankList?>

    @Insert
    suspend fun insertList(list: RankList): Long

    @Update
    suspend fun updateList(list: RankList)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    // Critérios
    @Query("SELECT * FROM criteria WHERE listId = :listId ORDER BY position")
    fun observeCriteria(listId: Long): Flow<List<Criterion>>

    @Query("SELECT * FROM criteria WHERE listId = :listId ORDER BY position")
    suspend fun getCriteria(listId: Long): List<Criterion>

    @Insert
    suspend fun insertCriterion(criterion: Criterion): Long

    @Update
    suspend fun updateCriterion(criterion: Criterion)

    @Query("DELETE FROM criteria WHERE id = :id")
    suspend fun deleteCriterion(id: Long)

    // Itens
    @Query("SELECT * FROM items WHERE listId = :listId")
    fun observeItems(listId: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItem(id: Long): Flow<Item?>

    @Insert
    suspend fun insertItem(item: Item): Long

    @Query("UPDATE items SET name = :name WHERE id = :id")
    suspend fun renameItem(id: Long, name: String)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Long): Item?

    @Query("UPDATE items SET photoPath = :path WHERE id = :id")
    suspend fun updatePhoto(id: Long, path: String?)

    @Query("SELECT photoPath FROM items WHERE listId = :listId AND photoPath IS NOT NULL")
    suspend fun getPhotoPaths(listId: Long): List<String>

    // Notas
    @Query(
        "SELECT scores.* FROM scores " +
            "INNER JOIN items ON items.id = scores.itemId " +
            "WHERE items.listId = :listId"
    )
    fun observeScoresForList(listId: Long): Flow<List<Score>>

    @Query("SELECT * FROM scores WHERE itemId = :itemId")
    fun observeScoresForItem(itemId: Long): Flow<List<Score>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScore(score: Score)

    @Query("DELETE FROM scores WHERE itemId = :itemId AND criterionId = :criterionId")
    suspend fun clearScore(itemId: Long, criterionId: Long)

    // Comentários
    @Query("SELECT * FROM comments WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun observeComments(itemId: Long): Flow<List<Comment>>

    @Query("SELECT * FROM comments WHERE itemId IN (:itemIds) ORDER BY createdAt DESC")
    fun observeCommentsFor(itemIds: List<Long>): Flow<List<Comment>>

    @Insert
    suspend fun insertComment(comment: Comment): Long

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: Long)
}

@Database(
    entities = [RankList::class, Criterion::class, Item::class, Score::class, Comment::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RankDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN photoPath TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ranking.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
