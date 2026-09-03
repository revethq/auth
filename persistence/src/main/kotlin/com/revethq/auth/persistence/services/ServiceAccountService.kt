package com.revethq.auth.persistence.services

import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.Pair
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.Scope
import com.revethq.auth.core.exceptions.notfound.ProfileNotFound
import com.revethq.auth.core.exceptions.notfound.ServiceAccountNotFound
import com.revethq.auth.core.services.SchemaService
import com.revethq.auth.core.services.ScopeService
import com.revethq.auth.persistence.entities.ScopeReference
import com.revethq.auth.persistence.entities.mappers.ProfileMapper
import com.revethq.auth.persistence.repositories.EventRepository
import com.revethq.auth.persistence.repositories.ProfileRepository
import com.revethq.auth.persistence.repositories.ScopeReferenceRepository
import com.revethq.core.Metadata
import com.revethq.core.SchemaValidation
import com.revethq.iam.serviceaccount.domain.ServiceAccount
import com.revethq.iam.serviceaccount.persistence.entity.ServiceAccountEntity
import com.revethq.iam.serviceaccount.persistence.repository.ServiceAccountRepository
import com.revethq.iam.user.domain.ProfileType
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ServiceAccountService(
    private val serviceAccountRepository: ServiceAccountRepository,
    private val profileRepository: ProfileRepository,
    private val scopeReferenceRepository: ScopeReferenceRepository,
    private val eventRepository: EventRepository,
    private val scopeService: ScopeService,
    private val schemaService: SchemaService
) : com.revethq.auth.core.services.ServiceAccountService {

    companion object {
        private val LOG = Logger.getLogger(ServiceAccountService::class.java)
    }

    @Transactional
    override fun getServiceAccount(serviceAccountId: UUID): Pair<ServiceAccount, Profile> {
        val entity = serviceAccountRepository.findById(serviceAccountId)
            ?: throw ServiceAccountNotFound()
        val profile = profileRepository
            .findByResourceAndProfileType(serviceAccountId, ProfileType.ServiceAccount)
            .map { ProfileMapper.from(it) }
            .orElseThrow { ProfileNotFound() }
        return Pair.create(entity.toDomain(), profile)
    }

    @Transactional
    override fun createServiceAccount(
        serviceAccount: ServiceAccount,
        profile: Profile,
        scopeIds: List<UUID>
    ): Pair<ServiceAccount, Profile> {
        val now = OffsetDateTime.now()
        val authorizationServerId = serviceAccount.tenantId?.let { UUID.fromString(it) }

        // Validate profile against OIDC schema
        validateProfileAndUpdateMetadata(serviceAccount, profile)

        val entity = ServiceAccountEntity.fromDomain(serviceAccount.copy(
            createdOn = now,
            updatedOn = now
        ))
        serviceAccountRepository.persist(entity)

        // Create profile
        profile.resource = entity.id
        profile.profileType = ProfileType.ServiceAccount
        profile.authorizationServerId = authorizationServerId
        profile.createdOn = now
        profile.updatedOn = now
        val profileEntity = ProfileMapper.to(profile)
        profileRepository.persist(profileEntity)

        // Create scope references
        for (scopeId in scopeIds) {
            val scopeReference = ScopeReference()
            scopeReference.scopeId = scopeId
            scopeReference.resourceId = entity.id
            scopeReference.scopeReferenceType = ScopeReference.ScopeReferenceType.SERVICE_ACCOUNT
            scopeReferenceRepository.persist(scopeReference)
        }

        serviceAccountRepository.flush()

        val created = serviceAccountRepository.findById(entity.id)!!
        return Pair.create(created.toDomain(), ProfileMapper.from(profileEntity))
    }

    @Transactional
    override fun updateServiceAccount(
        serviceAccount: ServiceAccount,
        profile: Profile?,
        scopeIds: List<UUID>?
    ): Pair<ServiceAccount, Profile> {
        val entity = serviceAccountRepository.findById(serviceAccount.id)
            ?: throw ServiceAccountNotFound()

        entity.name = serviceAccount.name
        entity.description = serviceAccount.description
        entity.metadata = serviceAccount.metadata
        entity.updatedOn = OffsetDateTime.now()
        serviceAccountRepository.persist(entity)

        // Update profile if provided
        if (profile?.profile != null) {
            val profileEntity = profileRepository
                .findByResourceAndProfileType(serviceAccount.id, ProfileType.ServiceAccount)
                .orElseThrow { ProfileNotFound() }
            profileEntity.profile = profile.profile
            profileEntity.updatedOn = OffsetDateTime.now()
            profileRepository.persist(profileEntity)
        }

        // Update scopes if provided
        if (scopeIds != null) {
            scopeReferenceRepository.delete(
                "resourceId = ?1 and scopeReferenceType = ?2",
                serviceAccount.id,
                ScopeReference.ScopeReferenceType.SERVICE_ACCOUNT
            )
            for (scopeId in scopeIds) {
                val scopeReference = ScopeReference()
                scopeReference.scopeId = scopeId
                scopeReference.resourceId = serviceAccount.id
                scopeReference.scopeReferenceType = ScopeReference.ScopeReferenceType.SERVICE_ACCOUNT
                scopeReferenceRepository.persist(scopeReference)
            }
        }

        val updatedProfile = profileRepository
            .findByResourceAndProfileType(serviceAccount.id, ProfileType.ServiceAccount)
            .map { ProfileMapper.from(it) }
            .orElseThrow { ProfileNotFound() }

        return Pair.create(entity.toDomain(), updatedProfile)
    }

    @Transactional
    override fun deleteServiceAccount(serviceAccountId: UUID) {
        val entity = serviceAccountRepository.findById(serviceAccountId)
            ?: throw ServiceAccountNotFound()

        // Delete scope references
        scopeReferenceRepository.delete(
            "resourceId = ?1 and scopeReferenceType = ?2",
            serviceAccountId,
            ScopeReference.ScopeReferenceType.SERVICE_ACCOUNT
        )

        // Delete profile
        profileRepository.deleteByResourceAndProfileType(serviceAccountId, ProfileType.ServiceAccount)

        serviceAccountRepository.delete(entity)

        LOG.info("Deleted service account: $serviceAccountId")
    }

    @Transactional
    override fun getServiceAccounts(authorizationServerIds: List<UUID>, page: Page): List<ServiceAccount> {
        val entities = if (authorizationServerIds.isEmpty()) {
            serviceAccountRepository.find("ORDER BY createdOn DESC")
                .page(page.offset() / page.limit(), page.limit())
                .list()
        } else {
            val tenantIds = authorizationServerIds.map { it.toString() }
            serviceAccountRepository.find("tenantId in ?1 ORDER BY createdOn DESC", tenantIds)
                .page(page.offset() / page.limit(), page.limit())
                .list()
        }
        return entities.map { it.toDomain() }
    }

    private fun validateProfileAndUpdateMetadata(serviceAccount: ServiceAccount, profile: Profile) {
        try {
            val authorizationServerId = serviceAccount.tenantId?.let { UUID.fromString(it) } ?: return

            val oidcSchema = schemaService.getSchemaByNameAndAuthorizationServerId(
                "oidc-userinfo-claims", authorizationServerId
            )

            if (oidcSchema != null) {
                val validationErrors = schemaService.validateProfileAgainstSchema(profile, oidcSchema)

                val schemaValidation = SchemaValidation(
                    oidcSchema.id,
                    validationErrors.isEmpty(),
                    OffsetDateTime.now()
                )

                val existingValidations = ArrayList(serviceAccount.metadata.schemaValidations)
                existingValidations.removeIf { sv -> oidcSchema.id == sv.schemaId }
                existingValidations.add(schemaValidation)

                serviceAccount.metadata = Metadata(
                    serviceAccount.metadata.identifiers,
                    existingValidations,
                    serviceAccount.metadata.properties
                )
            }
        } catch (e: Exception) {
            LOG.warn("Could not validate profile against OIDC schema: ${e.message}", e)
        }
    }
}
