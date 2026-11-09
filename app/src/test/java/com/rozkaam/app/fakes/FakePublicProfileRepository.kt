package com.rozkaam.app.fakes

import com.rozkaam.app.data.model.PublicProfile
import com.rozkaam.app.data.repository.PublicProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePublicProfileRepository(
    initialProfiles: Map<String, PublicProfile> = emptyMap()
) : PublicProfileRepository {

    val profilesFlow = MutableStateFlow(initialProfiles)

    override suspend fun getPublicProfile(uid: String): Result<PublicProfile?> {
        return Result.success(profilesFlow.value[uid])
    }

    override fun observePublicProfile(uid: String): Flow<PublicProfile?> {
        return profilesFlow.map { it[uid] }
    }
}

