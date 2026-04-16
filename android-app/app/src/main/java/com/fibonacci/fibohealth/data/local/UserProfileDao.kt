package com.fibonacci.fibohealth.data.local

import androidx.room.*
import com.fibonacci.fibohealth.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observe(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun get(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)
}
