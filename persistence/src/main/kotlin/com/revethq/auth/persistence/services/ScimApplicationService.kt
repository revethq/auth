/*
 * Copyright 2023 Bryce Groff (Revet)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.revethq.auth.persistence.services

import com.revethq.auth.core.domain.Credential
import com.revethq.auth.core.domain.CredentialType
import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.exceptions.badrequests.ScimApplicationInvalidScopes
import com.revethq.auth.core.exceptions.notfound.ScimApplicationNotFound
import com.revethq.auth.core.exceptions.notfound.ServiceAccountNotFound
import com.revethq.auth.core.scim.ScimOperation
import com.revethq.auth.core.scim.ScimScopes
import com.revethq.auth.core.services.CredentialService
import com.revethq.auth.core.services.ScimApplicationCreateResult
import com.revethq.auth.core.services.ScimScopeService
import com.revethq.auth.core.services.ServiceAccountService
import com.revethq.auth.persistence.entities.mappers.ScimApplicationMapper
import com.revethq.auth.persistence.repositories.ScimApplicationRepository
import com.revethq.auth.persistence.repositories.ScimDeliveryStatusRepository
import com.revethq.auth.persistence.repositories.ScimResourceMappingRepository
import com.revethq.iam.serviceaccount.domain.ServiceAccount
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ScimApplicationService(
    private val scimApplicationRepository: ScimApplicationRepository,
    private val scimDeliveryStatusRepository: ScimDeliveryStatusRepository,
    private val scimResourceMappingRepository: ScimResourceMappingRepository,
    private val serviceAccountService: ServiceAccountService,
    private val credentialService: CredentialService,
    private val scimScopeService: ScimScopeService
) : com.revethq.auth.core.services.ScimApplicationService {

    companion object {
        private val LOG = Logger.getLogger(ScimApplicationService::class.java)
        private val ALL_OPERATIONS = ScimOperation.entries.toSet()
    }

    @Transactional
    override fun getScimApplications(authorizationServerIds: List<UUID>, page: Page): List<ScimApplication> {
        val entities = if (authorizationServerIds.isEmpty()) {
            scimApplicationRepository.listAll(Sort.descending("createdOn"))
        } else {
            scimApplicationRepository.findByAuthorizationServerIdIn(authorizationServerIds)
        }

        val fromIndex = page.offset().coerceAtMost(entities.size)
        val toIndex = (page.offset() + page.limit()).coerceAtMost(entities.size)
        return entities.subList(fromIndex, toIndex).map { ScimApplicationMapper.from(it) }
    }

    @Transactional
    override fun getEnabledScimApplications(authorizationServerId: UUID): List<ScimApplication> {
        return scimApplicationRepository.findEnabledByAuthorizationServerId(authorizationServerId)
            .map { ScimApplicationMapper.from(it) }
    }

    @Transactional
    override fun getScimApplication(scimApplicationId: UUID): ScimApplication {
        return scimApplicationRepository.findByIdOptional(scimApplicationId)
            .map { ScimApplicationMapper.from(it) }
            .orElseThrow { ScimApplicationNotFound() }
    }

    @Transactional
    override fun createScimApplication(
        scimApplication: ScimApplication,
        autoCreateServiceAccount: Boolean
    ): ScimApplicationCreateResult {
        val authorizationServerId = scimApplication.authorizationServerId
            ?: throw IllegalArgumentException("Authorization server ID is required")

        // Ensure SCIM scopes exist for this authorization server
        ensureScimScopes(authorizationServerId)

        // Set default operations if not specified
        val enabledOperations = scimApplication.enabledOperations ?: ALL_OPERATIONS

        var credential: Credential? = null

        // Handle ServiceAccount association
        val serviceAccountId = if (scimApplication.serviceAccountId != null) {
            // Validate existing service account has required scopes
            validateServiceAccountScopes(scimApplication.serviceAccountId!!, enabledOperations)
            scimApplication.serviceAccountId!!
        } else if (autoCreateServiceAccount) {
            // Auto-create service account with SCIM scopes
            val (saId, secret) = createScimServiceAccount(
                authorizationServerId = authorizationServerId,
                scimAppName = scimApplication.name ?: "SCIM Application",
                operations = enabledOperations
            )
            credential = secret
            saId
        } else {
            throw IllegalArgumentException("Service account ID is required when autoCreateServiceAccount is false")
        }

        // Create the SCIM application
        val entity = ScimApplicationMapper.to(scimApplication.copy(
            serviceAccountId = serviceAccountId,
            enabledOperations = enabledOperations,
            attributeMapping = scimApplication.attributeMapping ?: ScimApplication.DEFAULT_USER_ATTRIBUTE_MAPPING,
            createdOn = OffsetDateTime.now(),
            updatedOn = OffsetDateTime.now()
        ))

        scimApplicationRepository.persist(entity)

        LOG.info("Created SCIM application: ${entity.id} for authorization server: $authorizationServerId")

        return ScimApplicationCreateResult(
            scimApplication = ScimApplicationMapper.from(entity),
            credential = credential
        )
    }

    @Transactional
    override fun updateScimApplication(scimApplication: ScimApplication): ScimApplication {
        val entity = scimApplicationRepository.findByIdOptional(scimApplication.id)
            .orElseThrow { ScimApplicationNotFound() }

        // Validate scopes if operations changed
        val newOperations = scimApplication.enabledOperations ?: entity.enabledOperations ?: ALL_OPERATIONS
        if (newOperations != entity.enabledOperations) {
            val serviceAccountId = scimApplication.serviceAccountId ?: entity.serviceAccountId
            if (serviceAccountId != null) {
                validateServiceAccountScopes(serviceAccountId, newOperations)
            }
        }

        // Update fields
        entity.name = scimApplication.name ?: entity.name
        entity.baseUrl = scimApplication.baseUrl ?: entity.baseUrl
        entity.attributeMapping = scimApplication.attributeMapping ?: entity.attributeMapping
        entity.enabledOperations = newOperations
        entity.deleteAction = scimApplication.deleteAction
        entity.retryPolicy = scimApplication.retryPolicy
        entity.enabled = scimApplication.enabled
        entity.updatedOn = OffsetDateTime.now()

        scimApplicationRepository.persist(entity)

        LOG.info("Updated SCIM application: ${entity.id}")

        return ScimApplicationMapper.from(entity)
    }

    @Transactional
    override fun deleteScimApplication(scimApplicationId: UUID) {
        val entity = scimApplicationRepository.findByIdOptional(scimApplicationId)
            .orElseThrow { ScimApplicationNotFound() }

        // Delete related records
        scimResourceMappingRepository.deleteByScimApplicationId(scimApplicationId)
        // Note: We keep delivery status records for audit purposes

        scimApplicationRepository.delete(entity)

        LOG.info("Deleted SCIM application: $scimApplicationId")
    }

    @Transactional
    override fun ensureScimScopes(authorizationServerId: UUID) {
        scimScopeService.ensureScimScopes(authorizationServerId)
    }

    private fun validateServiceAccountScopes(serviceAccountId: UUID, operations: Set<ScimOperation>) {
        if (!scimScopeService.validateServiceAccountScopes(serviceAccountId, operations)) {
            val requiredScopeNames = ScimScopes.getRequiredScopes(operations)
            throw ScimApplicationInvalidScopes(requiredScopeNames)
        }
    }

    private fun createScimServiceAccount(
        authorizationServerId: UUID,
        scimAppName: String,
        operations: Set<ScimOperation>
    ): Pair<UUID, Credential> {
        // Get the required scopes
        val requiredScopes = scimScopeService.getScopesForOperations(authorizationServerId, operations)

        // Create the ServiceAccount
        val serviceAccount = ServiceAccount(
            id = UUID.randomUUID(),
            name = "$scimAppName SCIM Client",
            tenantId = authorizationServerId.toString()
        )

        val profile = Profile()
        profile.profile = emptyMap()

        val result = serviceAccountService.createServiceAccount(
            serviceAccount, profile, requiredScopes.mapNotNull { it.id }
        )
        val createdSa = result.left ?: throw RuntimeException("Failed to create service account")

        // Create a Credential for the service account
        val credential = Credential(
            principalId = createdSa.id,
            authorizationServerId = authorizationServerId,
            type = CredentialType.API_KEY,
            name = "SCIM Client Secret",
            scopes = requiredScopes
        )

        val createdCredential = credentialService.createCredential(credential)

        LOG.info("Auto-created ServiceAccount ${createdSa.id} with SCIM scopes for SCIM application")

        return Pair(createdSa.id, createdCredential)
    }
}
