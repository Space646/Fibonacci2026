package com.fibonacci.fibohealth.data.repository

import com.fibonacci.fibohealth.data.local.UserProfileDao
import com.fibonacci.fibohealth.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val dao: UserProfileDao) {

    val profile: Flow<UserProfile> = dao.observe().map { it ?: UserProfile() }

    suspend fun save(profile: UserProfile) = dao.upsert(profile)

    suspend fun getOrDefault(): UserProfile = dao.get() ?: UserProfile()
}
