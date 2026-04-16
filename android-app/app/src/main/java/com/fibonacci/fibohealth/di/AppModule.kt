package com.fibonacci.fibohealth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fibonacci.fibohealth.data.local.FiboHealthDatabase
import com.fibonacci.fibohealth.data.local.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FiboHealthDatabase =
        Room.databaseBuilder(ctx, FiboHealthDatabase::class.java, "fibohealth.db").build()

    @Provides
    fun provideUserProfileDao(db: FiboHealthDatabase): UserProfileDao = db.userProfileDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore
}
