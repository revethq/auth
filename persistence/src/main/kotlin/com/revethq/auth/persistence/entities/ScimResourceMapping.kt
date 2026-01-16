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

package com.revethq.auth.persistence.entities

import com.revethq.auth.core.domain.ResourceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA entity for mapping local resources to downstream SCIM resource IDs.
 */
@Entity
@Table(
    name = "scim_resource_mapping",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_scim_mapping_local_resource",
            columnNames = ["scimApplicationId", "localResourceType", "localResourceId"]
        )
    ],
    indexes = [
        Index(name = "idx_scim_mapping_app", columnList = "scimApplicationId"),
        Index(name = "idx_scim_mapping_local", columnList = "localResourceType, localResourceId")
    ]
)
open class ScimResourceMapping {
    @Id
    @GeneratedValue
    open var id: UUID? = null

    @Column(nullable = false)
    open var scimApplicationId: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var localResourceType: ResourceType? = null

    @Column(nullable = false)
    open var localResourceId: UUID? = null

    /**
     * The resource ID assigned by the downstream SCIM server.
     */
    @Column(nullable = false)
    open var scimResourceId: String? = null

    open var createdOn: OffsetDateTime? = null
}
