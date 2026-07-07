package luzzr.xi.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import luzzr.xi.core.datastore.dataStore
import luzzr.xi.data.repository.TranslationRepository
import luzzr.xi.data.repository.EssayRepository
import luzzr.xi.data.repository.ApiRepositoryImpl
import luzzr.xi.domain.repository.TranslationGateway
import luzzr.xi.domain.repository.EssayGateway
import luzzr.xi.domain.repository.SettingsGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideTranslationGateway(impl: TranslationRepository): TranslationGateway = impl

    @Provides
    @Singleton
    fun provideEssayGateway(impl: EssayRepository): EssayGateway = impl

    @Provides
    @Singleton
    fun provideSettingsGateway(impl: ApiRepositoryImpl): SettingsGateway = impl
}
