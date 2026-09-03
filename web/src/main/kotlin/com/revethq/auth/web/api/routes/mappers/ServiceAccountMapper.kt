package com.revethq.auth.web.api.routes.mappers

import com.revethq.auth.core.api.dto.ServiceAccountRequest
import com.revethq.auth.core.api.dto.ServiceAccountResponse
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.Scope
import com.revethq.core.Metadata
import com.revethq.iam.serviceaccount.domain.ServiceAccount
import java.util.UUID

object ServiceAccountMapper {

    @JvmStatic
    fun from(request: ServiceAccountRequest): ServiceAccount {
        return ServiceAccount(
            id = UUID.randomUUID(),
            name = request.name ?: "",
            description = request.description,
            tenantId = request.authorizationServerId?.toString(),
            metadata = MetadataMapper.from(request.metadata)
        )
    }

    @JvmStatic
    fun toResponse(serviceAccount: ServiceAccount): ServiceAccountResponse {
        return ServiceAccountResponse(
            id = serviceAccount.id,
            authorizationServerId = serviceAccount.tenantId?.let { UUID.fromString(it) },
            name = serviceAccount.name,
            description = serviceAccount.description,
            metadata = MetadataMapper.to(serviceAccount.metadata),
            createdOn = serviceAccount.createdOn,
            updatedOn = serviceAccount.updatedOn
        )
    }

    @JvmStatic
    fun toResponseWithProfile(serviceAccount: ServiceAccount, profile: Profile): ServiceAccountResponse {
        return ServiceAccountResponse(
            id = serviceAccount.id,
            authorizationServerId = serviceAccount.tenantId?.let { UUID.fromString(it) },
            name = serviceAccount.name,
            description = serviceAccount.description,
            metadata = MetadataMapper.to(serviceAccount.metadata),
            createdOn = serviceAccount.createdOn,
            updatedOn = serviceAccount.updatedOn,
            profile = profile.profile
        )
    }
}
