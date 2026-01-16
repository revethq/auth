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

package com.revethq.auth.persistence.scim.operations

import com.revethq.auth.core.domain.ResourceType
import com.revethq.auth.core.domain.ScimResourceMapping
import com.revethq.auth.persistence.entities.mappers.ScimResourceMappingMapper
import com.revethq.auth.persistence.repositories.ScimResourceMappingRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Service for managing SCIM resource ID mappings.
 */
@ApplicationScoped
class ScimResourceMappingService(
    private val scimResourceMappingRepository: ScimResourceMappingRepository
) {

    companion object {
        private val LOG = Logger.getLogger(ScimResourceMappingService::class.java)
    }

    /**
     * Get the SCIM resource ID for a local resource.
     *
     * @param scimApplicationId The SCIM application ID
     * @param localResourceType The local resource type (USER, GROUP)
     * @param localResourceId The local resource ID
     * @return The SCIM resource ID, or null if not found
     */
    @Transactional
    fun getScimResourceId(
        scimApplicationId: UUID,
        localResourceType: ResourceType,
        localResourceId: UUID
    ): String? {
        return scimResourceMappingRepository.findByScimApplicationAndLocalResource(
            scimApplicationId,
            localResourceType,
            localResourceId
        ).map { it.scimResourceId }.orElse(null)
    }

    /**
     * Save a SCIM resource mapping.
     *
     * @param scimApplicationId The SCIM application ID
     * @param localResourceType The local resource type
     * @param localResourceId The local resource ID
     * @param scimResourceId The SCIM resource ID from the downstream server
     */
    @Transactional
    fun saveMapping(
        scimApplicationId: UUID,
        localResourceType: ResourceType,
        localResourceId: UUID,
        scimResourceId: String
    ): ScimResourceMapping {
        // Check if mapping already exists
        val existingMapping = scimResourceMappingRepository.findByScimApplicationAndLocalResource(
            scimApplicationId,
            localResourceType,
            localResourceId
        )

        if (existingMapping.isPresent) {
            // Update existing mapping
            val entity = existingMapping.get()
            entity.scimResourceId = scimResourceId
            scimResourceMappingRepository.persist(entity)

            LOG.debug("Updated SCIM mapping for $localResourceType:$localResourceId -> $scimResourceId")
            return ScimResourceMappingMapper.from(entity)
        }

        // Create new mapping
        val mapping = ScimResourceMapping(
            scimApplicationId = scimApplicationId,
            localResourceType = localResourceType,
            localResourceId = localResourceId,
            scimResourceId = scimResourceId,
            createdOn = OffsetDateTime.now()
        )

        val entity = ScimResourceMappingMapper.to(mapping)
        scimResourceMappingRepository.persist(entity)

        LOG.debug("Created SCIM mapping for $localResourceType:$localResourceId -> $scimResourceId")
        return ScimResourceMappingMapper.from(entity)
    }

    /**
     * Delete a SCIM resource mapping.
     *
     * @param scimApplicationId The SCIM application ID
     * @param localResourceType The local resource type
     * @param localResourceId The local resource ID
     */
    @Transactional
    fun deleteMapping(
        scimApplicationId: UUID,
        localResourceType: ResourceType,
        localResourceId: UUID
    ) {
        val deleted = scimResourceMappingRepository.deleteByScimApplicationAndLocalResource(
            scimApplicationId,
            localResourceType,
            localResourceId
        )

        if (deleted > 0) {
            LOG.debug("Deleted SCIM mapping for $localResourceType:$localResourceId")
        }
    }

    /**
     * Get all SCIM resource IDs for a local resource across all SCIM applications.
     */
    @Transactional
    fun getMappingsForLocalResource(
        localResourceType: ResourceType,
        localResourceId: UUID
    ): List<ScimResourceMapping> {
        return scimResourceMappingRepository.findByLocalResource(localResourceType, localResourceId)
            .map { ScimResourceMappingMapper.from(it) }
    }
}
