package luzzr.xi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
