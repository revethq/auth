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

import com.revethq.auth.core.domain.Application
import com.revethq.auth.core.domain.ApplicationSecret
import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.exceptions.badrequests.ScimApplicationInvalidScopes
import com.revethq.auth.core.exceptions.notfound.ApplicationNotFound
import com.revethq.auth.core.exceptions.notfound.ScimApplicationNotFound
import com.revethq.auth.core.scim.ScimOperation
import com.revethq.auth.core.scim.ScimScopes
import com.revethq.auth.core.services.ApplicationService
import com.revethq.auth.core.services.ScimApplicationCreateResult
import com.revethq.auth.core.services.ScimScopeService
import com.revethq.auth.persistence.entities.mappers.ScimApplicationMapper
import com.revethq.auth.persistence.repositories.ApplicationRepository
import com.revethq.auth.persistence.repositories.ScimApplicationRepository
import com.revethq.auth.persistence.repositories.ScimDeliveryStatusRepository
import com.revethq.auth.persistence.repositories.ScimResourceMappingRepository
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
    private val applicationRepository: ApplicationRepository,
    private val applicationService: ApplicationService,
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
        autoCreateApplication: Boolean
    ): ScimApplicationCreateResult {
        val authorizationServerId = scimApplication.authorizationServerId
            ?: throw IllegalArgumentException("Authorization server ID is required")

        // Ensure SCIM scopes exist for this authorization server
        ensureScimScopes(authorizationServerId)

        // Set default operations if not specified
        val enabledOperations = scimApplication.enabledOperations ?: ALL_OPERATIONS

        var applicationSecret: ApplicationSecret? = null

        // Handle Application association
        val applicationId = if (scimApplication.applicationId != null) {
            // Validate existing application has required scopes
            validateApplicationScopes(scimApplication.applicationId!!, enabledOperations)
            scimApplication.applicationId!!
        } else if (autoCreateApplication) {
            // Auto-create application with SCIM scopes
            val (appId, secret) = createScimApplication(
                authorizationServerId = authorizationServerId,
                scimAppName = scimApplication.name ?: "SCIM Application",
                operations = enabledOperations
            )
            applicationSecret = secret
            appId
        } else {
            throw IllegalArgumentException("Application ID is required when autoCreateApplication is false")
        }

        // Create the SCIM application
        val entity = ScimApplicationMapper.to(scimApplication.copy(
            applicationId = applicationId,
            enabledOperations = enabledOperations,
            attributeMapping = scimApplication.attributeMapping ?: ScimApplication.DEFAULT_USER_ATTRIBUTE_MAPPING,
            createdOn = OffsetDateTime.now(),
            updatedOn = OffsetDateTime.now()
        ))

        scimApplicationRepository.persist(entity)

        LOG.info("Created SCIM application: ${entity.id} for authorization server: $authorizationServerId")

        return ScimApplicationCreateResult(
            scimApplication = ScimApplicationMapper.from(entity),
            applicationSecret = applicationSecret
        )
    }

    @Transactional
    override fun updateScimApplication(scimApplication: ScimApplication): ScimApplication {
        val entity = scimApplicationRepository.findByIdOptional(scimApplication.id)
            .orElseThrow { ScimApplicationNotFound() }

        // Validate scopes if operations changed
        val newOperations = scimApplication.enabledOperations ?: entity.enabledOperations ?: ALL_OPERATIONS
        if (newOperations != entity.enabledOperations) {
            val applicationId = scimApplication.applicationId ?: entity.applicationId
            if (applicationId != null) {
                validateApplicationScopes(applicationId, newOperations)
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

    private fun validateApplicationScopes(applicationId: UUID, operations: Set<ScimOperation>) {
        val application = applicationRepository.findByIdOptional(applicationId)
            .orElseThrow { ApplicationNotFound() }

        val applicationScopeNames = application.scopes.mapNotNull { it.name }.toSet()
        val requiredScopeNames = ScimScopes.getRequiredScopes(operations)
        val missingScopeNames = requiredScopeNames - applicationScopeNames

        if (missingScopeNames.isNotEmpty()) {
            throw ScimApplicationInvalidScopes(missingScopeNames)
        }
    }

    private fun createScimApplication(
        authorizationServerId: UUID,
        scimAppName: String,
        operations: Set<ScimOperation>
    ): Pair<UUID, ApplicationSecret> {
        // Get the required scopes
        val requiredScopes = scimScopeService.getScopesForOperations(authorizationServerId, operations)

        // Create the Application
        val application = Application(
            authorizationServerId = authorizationServerId,
            clientId = "scim-client-${UUID.randomUUID().toString().take(8)}",
            name = "$scimAppName SCIM Client",
            scopes = requiredScopes
        )

        val profile = Profile()
        profile.profile = emptyMap()

        val result = applicationService.createApplication(application, profile)
        val createdApp = result.left ?: throw RuntimeException("Failed to create application")

        // Create an ApplicationSecret
        val secret = ApplicationSecret(
            applicationId = createdApp.id,
            name = "SCIM Client Secret",
            scopes = requiredScopes
        )

        val createdSecret = applicationService.createApplicationSecret(secret)

        LOG.info("Auto-created Application ${createdApp.id} with SCIM scopes for SCIM application")

        return Pair(createdApp.id!!, createdSecret)
    }
}
