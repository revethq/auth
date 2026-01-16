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

package com.revethq.auth.persistence.repositories

import com.revethq.auth.core.domain.ResourceType
import com.revethq.auth.persistence.entities.ScimResourceMapping
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.Optional
import java.util.UUID

@ApplicationScoped
class ScimResourceMappingRepository : PanacheRepositoryBase<ScimResourceMapping, UUID> {

    /**
     * Find the SCIM resource ID for a local resource.
     */
    fun findByScimApplicationAndLocalResource(
        scimApplicationId: UUID,
        localResourceType: ResourceType,
        localResourceId: UUID
    ): Optional<ScimResourceMapping> {
        return find(
            "scimApplicationId = ?1 and localResourceType = ?2 and localResourceId = ?3",
            scimApplicationId,
            localResourceType,
            localResourceId
        ).firstResultOptional()
    }

    /**
     * Find all mappings for a SCIM application.
     */
    fun findByScimApplicationId(scimApplicationId: UUID): List<ScimResourceMapping> {
        return find("scimApplicationId", scimApplicationId).list()
    }

    /**
     * Find all mappings for a local resource across all SCIM applications.
     */
    fun findByLocalResource(localResourceType: ResourceType, localResourceId: UUID): List<ScimResourceMapping> {
        return find(
            "localResourceType = ?1 and localResourceId = ?2",
            localResourceType,
            localResourceId
        ).list()
    }

    /**
     * Delete all mappings for a SCIM application.
     */
    fun deleteByScimApplicationId(scimApplicationId: UUID): Long {
        return delete("scimApplicationId", scimApplicationId)
    }

    /**
     * Delete mapping for a specific local resource in a specific SCIM application.
     */
    fun deleteByScimApplicationAndLocalResource(
        scimApplicationId: UUID,
        localResourceType: ResourceType,
        localResourceId: UUID
    ): Long {
        return delete(
            "scimApplicationId = ?1 and localResourceType = ?2 and localResourceId = ?3",
            scimApplicationId,
            localResourceType,
            localResourceId
        )
    }
}
