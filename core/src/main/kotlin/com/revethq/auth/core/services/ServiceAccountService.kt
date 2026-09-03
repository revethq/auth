package com.revethq.auth.core.services

import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.Pair
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.exceptions.notfound.ProfileNotFound
import com.revethq.auth.core.exceptions.notfound.ServiceAccountNotFound
import com.revethq.iam.serviceaccount.domain.ServiceAccount
import java.util.UUID

interface ServiceAccountService {

    @Throws(ServiceAccountNotFound::class, ProfileNotFound::class)
    fun getServiceAccount(serviceAccountId: UUID): Pair<ServiceAccount, Profile>

    fun createServiceAccount(serviceAccount: ServiceAccount, profile: Profile, scopeIds: List<UUID>): Pair<ServiceAccount, Profile>

    fun updateServiceAccount(serviceAccount: ServiceAccount, profile: Profile?, scopeIds: List<UUID>?): Pair<ServiceAccount, Profile>

    @Throws(ServiceAccountNotFound::class)
    fun deleteServiceAccount(serviceAccountId: UUID)

    fun getServiceAccounts(authorizationServerIds: List<UUID>, page: Page): List<ServiceAccount>
}
