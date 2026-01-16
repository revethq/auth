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

package com.revethq.auth.persistence.entities.mappers

import com.revethq.auth.core.domain.ScimApplication as DomainScimApplication
import com.revethq.auth.persistence.entities.ScimApplication as EntityScimApplication

/**
 * Mapper between domain ScimApplication and JPA entity ScimApplication.
 */
object ScimApplicationMapper {

    @JvmStatic
    fun from(entity: EntityScimApplication): DomainScimApplication {
        return DomainScimApplication(
            id = entity.id,
            authorizationServerId = entity.authorizationServerId,
            applicationId = entity.applicationId,
            name = entity.name,
            baseUrl = entity.baseUrl,
            attributeMapping = entity.attributeMapping,
            enabledOperations = entity.enabledOperations,
            deleteAction = entity.deleteAction,
            retryPolicy = entity.retryPolicy,
            enabled = entity.enabled,
            createdOn = entity.createdOn,
            updatedOn = entity.updatedOn
        )
    }

    @JvmStatic
    fun to(domain: DomainScimApplication): EntityScimApplication {
        return EntityScimApplication().apply {
            id = domain.id
            authorizationServerId = domain.authorizationServerId
            applicationId = domain.applicationId
            name = domain.name
            baseUrl = domain.baseUrl
            attributeMapping = domain.attributeMapping
            enabledOperations = domain.enabledOperations
            deleteAction = domain.deleteAction
            retryPolicy = domain.retryPolicy
            enabled = domain.enabled
            createdOn = domain.createdOn
            updatedOn = domain.updatedOn
        }
    }
}
