package com.revethq.auth.web.api.routes

import com.revethq.auth.core.api.dto.ServiceAccountRequest
import com.revethq.auth.core.api.dto.ServiceAccountsResponse
import com.revethq.auth.core.api.interfaces.ServiceAccountsApi
import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.services.ServiceAccountService
import com.revethq.auth.web.api.routes.Constants.LIMIT_DEFAULT
import com.revethq.auth.web.api.routes.Constants.OFFSET_DEFAULT
import com.revethq.auth.web.api.routes.Pagination.getPage
import com.revethq.auth.web.api.routes.mappers.ServiceAccountMapper
import com.revethq.iam.serviceaccount.domain.ServiceAccount
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.UUID

@ApplicationScoped
open class ServiceAccounts @Inject constructor(
    private val serviceAccountService: ServiceAccountService
) : ServiceAccountsApi {

    override fun createServiceAccount(serviceAccountRequest: ServiceAccountRequest): Response {
        @Suppress("UNCHECKED_CAST")
        val profileMap = serviceAccountRequest.profile as? Map<String, Any> ?: emptyMap()
        val profile = Profile().apply {
            this.profile = profileMap
        }

        val serviceAccount = ServiceAccountMapper.from(serviceAccountRequest)
        val scopeIds = serviceAccountRequest.scopes ?: emptyList()

        val result = serviceAccountService.createServiceAccount(serviceAccount, profile, scopeIds)
        val sa = result.left ?: throw IllegalArgumentException("Service account not found")
        val createdProfile = result.right ?: throw IllegalArgumentException("Profile not found")

        return Response
            .status(Response.Status.CREATED)
            .entity(ServiceAccountMapper.toResponseWithProfile(sa, createdProfile))
            .build()
    }

    override fun deleteServiceAccount(serviceAccountId: UUID): Response {
        serviceAccountService.deleteServiceAccount(serviceAccountId)
        return Response.noContent().build()
    }

    override fun getServiceAccount(serviceAccountId: UUID): Response {
        val result = serviceAccountService.getServiceAccount(serviceAccountId)
        val sa = result.left ?: throw IllegalArgumentException("Service account not found")
        val profile = result.right ?: throw IllegalArgumentException("Profile not found")
        return Response
            .ok()
            .entity(ServiceAccountMapper.toResponseWithProfile(sa, profile))
            .build()
    }

    override fun updateServiceAccount(serviceAccountId: UUID, serviceAccountRequest: ServiceAccountRequest): Response {
        @Suppress("UNCHECKED_CAST")
        val profileMap = serviceAccountRequest.profile as? Map<String, Any>
        val profile = profileMap?.let {
            Profile().apply { this.profile = it }
        }

        val serviceAccount = ServiceAccount(
            id = serviceAccountId,
            name = serviceAccountRequest.name ?: "",
            description = serviceAccountRequest.description,
            tenantId = serviceAccountRequest.authorizationServerId?.toString()
        )

        val result = serviceAccountService.updateServiceAccount(
            serviceAccount, profile, serviceAccountRequest.scopes
        )
        val sa = result.left ?: throw IllegalArgumentException("Service account not found")
        val updatedProfile = result.right ?: throw IllegalArgumentException("Profile not found")

        return Response
            .ok()
            .entity(ServiceAccountMapper.toResponseWithProfile(sa, updatedProfile))
            .build()
    }

    override fun listServiceAccounts(
        authorizationServerIds: List<UUID>?,
        limit: Int?,
        offset: Int?
    ): Response {
        val actualLimit = limit ?: LIMIT_DEFAULT
        val actualOffset = offset ?: OFFSET_DEFAULT
        val serverIds = authorizationServerIds ?: emptyList()

        val serviceAccountsResponse = ServiceAccountsResponse(
            serviceAccounts = serviceAccountService
                .getServiceAccounts(serverIds, Page(actualLimit, actualOffset))
                .map { ServiceAccountMapper.toResponse(it) },
            page = getPage("service-accounts", serverIds, actualLimit, actualOffset)
        )
        return Response.ok().entity(serviceAccountsResponse).build()
    }
}
