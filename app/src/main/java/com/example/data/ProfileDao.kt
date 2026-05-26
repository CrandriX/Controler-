package com.example.data

import androidx.room.*
import com.example.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM controller_profiles ORDER BY id ASC")
    fun getAllProfilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM controller_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): Profile?

    @Query("SELECT * FROM controller_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Transaction
    suspend fun setDefaultProfile(profileId: Long) {
        // Clear old default
        clearDefaults()
        // Set new default
        makeDefault(profileId)
    }

    @Query("UPDATE controller_profiles SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE controller_profiles SET isDefault = 1 WHERE id = :profileId")
    suspend fun makeDefault(profileId: Long)
}
