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

import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.Pair
import com.revethq.auth.core.domain.Application
import com.revethq.core.Metadata
import com.revethq.auth.core.domain.Profile
import com.revethq.iam.user.domain.ProfileType
import com.revethq.core.SchemaValidation
import com.revethq.auth.core.services.SchemaService
import com.revethq.auth.core.exceptions.notfound.ApplicationNotFound
import com.revethq.auth.core.exceptions.notfound.ProfileNotFound
import com.revethq.auth.core.services.ScopeService
import com.revethq.auth.persistence.repositories.*
import com.revethq.auth.persistence.entities.EventType
import com.revethq.auth.persistence.entities.ScopeReference
import com.revethq.auth.persistence.entities.mappers.ApplicationMapper
import com.revethq.auth.persistence.entities.mappers.ProfileMapper
import io.quarkus.hibernate.orm.panache.PanacheQuery
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val eventRepository: EventRepository,
    private val profileRepository: ProfileRepository,
    private val scopeReferenceRepository: ScopeReferenceRepository,
    private val scopeService: ScopeService,
    private val schemaService: SchemaService
) : com.revethq.auth.core.services.ApplicationService {

    companion object {
        private val LOG = Logger.getLogger(ApplicationService::class.java)
    }

    @Transactional
    @Throws(ApplicationNotFound::class)
    override fun deleteApplication(applicationId: UUID) {
        val application = applicationRepository
            .findByIdOptional(applicationId)
            .orElseThrow { ApplicationNotFound() }
        val profile = profileRepository
            .findByResourceAndProfileType(applicationId, ProfileType.Application)
            .orElseThrow { ProfileNotFound() }
        applicationRepository.delete(application)
        profileRepository.delete(profile)
        eventRepository.createApplicationProfileEvent(
            Pair.create(ApplicationMapper.from(application), ProfileMapper.from(profile)),
            EventType.DELETE
        )
    }

    @Transactional
    @Throws(ApplicationNotFound::class, ProfileNotFound::class)
    override fun getApplication(applicationId: UUID): Pair<Application, Profile> {
        val application = applicationRepository
            .findByIdOptional(applicationId)
            .map { ApplicationMapper.from(it) }
            .orElseThrow { ApplicationNotFound() }
        val profile = profileRepository
            .findByResourceAndProfileType(applicationId, ProfileType.Application)
            .map { ProfileMapper.from(it) }
            .orElseThrow { ProfileNotFound() }
        return Pair.create(application, profile)
    }

    @Transactional
    override fun createApplication(application: Application, profile: Profile): Pair<Application, Profile> {
        // Generate ID for application since it doesn't use @GeneratedValue
        if (application.id == null) {
            application.id = UUID.randomUUID()
        }

        application.createdOn = OffsetDateTime.now()
        application.updatedOn = OffsetDateTime.now()

        // Validate profile against OIDC schema and store validation results in application metadata
        validateProfileAndUpdateApplicationMetadata(application, profile)

        val _application = ApplicationMapper.to(application)
        applicationRepository.persist(_application)

        profile.resource = _application.id
        profile.profileType = ProfileType.Application
        profile.authorizationServerId = _application.authorizationServerId
        profile.createdOn = OffsetDateTime.now()
        profile.updatedOn = OffsetDateTime.now()
        val _profile = ProfileMapper.to(profile)
        profileRepository.persist(_profile)

        for (scope in application.scopes.orEmpty()) {
            val scopeReference = ScopeReference()
            scopeReference.scopeId = scope.id
            scopeReference.resourceId = _application.id
            scopeReference.scopeReferenceType = ScopeReference.ScopeReferenceType.APPLICATION
            scopeReferenceRepository.persist(scopeReference)
        }

        // Flush to ensure scopes are persisted before creating the event
        applicationRepository.flush()

        val applicationProfilePair = Pair.create(
            ApplicationMapper.from(applicationRepository.findById(_application.id)),
            ProfileMapper.from(_profile)
        )
        eventRepository.createApplicationProfileEvent(applicationProfilePair, EventType.CREATE)
        return applicationProfilePair
    }

    @Transactional
    @Suppress("UNCHECKED_CAST")
    override fun getApplications(authorizationServerIds: List<UUID>, page: Page): List<Application> {
        val entities: List<com.revethq.auth.persistence.entities.Application>
        if (authorizationServerIds.isEmpty()) {
            val query: PanacheQuery<com.revethq.auth.persistence.entities.Application> = applicationRepository.findAll(Sort.descending("createdOn"))
            val paged: PanacheQuery<com.revethq.auth.persistence.entities.Application> = query.page(page.offset() / page.limit(), page.limit())
            entities = paged.list()
        } else {
            val query: PanacheQuery<com.revethq.auth.persistence.entities.Application> = applicationRepository.find("authorizationServerId in ?1", Sort.descending("createdOn"), authorizationServerIds)
            val paged: PanacheQuery<com.revethq.auth.persistence.entities.Application> = query.page(page.offset() / page.limit(), page.limit())
            entities = paged.list()
        }
        return entities.map { ApplicationMapper.from(it) }
    }

    private fun validateProfileAndUpdateApplicationMetadata(application: Application, profile: Profile) {
        try {
            // Find the OIDC UserInfo claims schema for this authorization server
            val oidcSchema = application.authorizationServerId?.let {
                schemaService.getSchemaByNameAndAuthorizationServerId("oidc-userinfo-claims", it)
            }

            if (oidcSchema != null) {
                // Validate the profile against the schema
                val validationErrors = schemaService.validateProfileAgainstSchema(profile, oidcSchema)

                // Create schema validation result
                val schemaValidation = SchemaValidation(
                    oidcSchema.id,
                    validationErrors.isEmpty(),
                    OffsetDateTime.now()
                )

                // Build the updated schema validations list
                val currentMetadata = application.metadata
                val existingValidations = if (currentMetadata?.schemaValidations != null)
                    ArrayList(currentMetadata.schemaValidations)
                else
                    ArrayList<SchemaValidation>()

                // Remove any existing validation for this schema
                existingValidations.removeIf { sv -> oidcSchema.id == sv.schemaId }

                // Add the new validation result
                existingValidations.add(schemaValidation)

                // Create new metadata with updated schema validations
                val existingMetadata = application.metadata
                val newMetadata = Metadata(
                    existingMetadata?.identifiers,
                    existingValidations,
                    existingMetadata?.properties
                )
                application.metadata = newMetadata
            }
        } catch (e: Exception) {
            // Log warning but don't fail application creation if validation fails
            LOG.warn("Could not validate profile against OIDC schema: ${e.message}", e)
        }
    }
}
