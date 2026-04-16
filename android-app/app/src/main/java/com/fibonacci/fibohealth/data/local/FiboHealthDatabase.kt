package com.fibonacci.fibohealth.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fibonacci.fibohealth.data.model.UserProfile

@Database(entities = [UserProfile::class], version = 1, exportSchema = true)
abstract class FiboHealthDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}
