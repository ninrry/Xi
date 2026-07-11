package luzzr.xi.core.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import luzzr.xi.data.local.AppDatabase
import luzzr.xi.data.local.HistoryDao
import luzzr.xi.data.repository.HistoryRepository
import luzzr.xi.domain.repository.HistoryGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "xi_history.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideHistoryGateway(impl: HistoryRepository): HistoryGateway = impl
}
