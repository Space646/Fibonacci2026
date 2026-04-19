package com.fibonacci.fibohealth.data.repository

import com.fibonacci.fibohealth.data.local.UserProfileDao
import com.fibonacci.fibohealth.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val dao: UserProfileDao) {

    val profile: Flow<UserProfile> = dao.observe().map { it ?: UserProfile() }

    suspend fun save(profile: UserProfile) = dao.upsert(profile)

    /**
     * Returns the stored profile, minting and persisting a per-install
     * `deviceId` UUID if one isn't present yet. The Pi rejects BLE writes
     * whose JSON has an empty `device_id`, and the Profile UI has no field
     * for it — so this is the single choke point that guarantees every read
     * path (BleService startup, ActivityViewModel, ProfileViewModel) sees a
     * valid id without waiting for the user to tap "Save & Sync". Matches
     * iOS's UserProfile.init behaviour.
     */
    suspend fun getOrDefault(): UserProfile {
        val current = dao.get() ?: UserProfile()
        if (current.deviceId.isNotBlank()) return current
        val minted = current.copy(deviceId = UUID.randomUUID().toString())
        dao.upsert(minted)
        return minted
    }
}
