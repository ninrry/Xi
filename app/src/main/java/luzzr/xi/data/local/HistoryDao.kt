package luzzr.xi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(record: HistoryRecordEntity): Long

    @Query("SELECT * FROM history_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryRecordEntity>>

    @Query("SELECT * FROM history_records WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<HistoryRecordEntity>>

    @Query("SELECT * FROM history_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryRecordEntity?

    @Query("SELECT COUNT(*) FROM history_records WHERE type = :type AND createdAt >= :since")
    suspend fun countByTypeSince(type: String, since: Long): Int

    @Query("SELECT * FROM history_records WHERE type = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestByType(type: String): HistoryRecordEntity?

    @Query("DELETE FROM history_records")
    suspend fun deleteAll()

    @Query("DELETE FROM history_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        DELETE FROM history_records WHERE id NOT IN (
            SELECT id FROM history_records ORDER BY createdAt DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int)
}
