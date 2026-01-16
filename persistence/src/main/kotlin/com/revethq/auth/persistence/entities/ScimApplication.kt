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

import com.revethq.auth.core.scim.RetryPolicy
import com.revethq.auth.core.scim.ScimDeleteAction
import com.revethq.auth.core.scim.ScimOperation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA entity for SCIM application configuration.
 */
@Entity
@Table(
    name = "scim_application",
    indexes = [
        Index(name = "idx_scim_app_auth_server", columnList = "authorizationServerId"),
        Index(name = "idx_scim_app_application", columnList = "applicationId"),
        Index(name = "idx_scim_app_enabled", columnList = "enabled")
    ]
)
open class ScimApplication {
    @Id
    @GeneratedValue
    open var id: UUID? = null

    @Column(nullable = false)
    open var authorizationServerId: UUID? = null

    @Column(nullable = false)
    open var applicationId: UUID? = null

    @Column(nullable = false)
    open var name: String? = null

    @Column(nullable = false)
    open var baseUrl: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    open var attributeMapping: Map<String, String>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    open var enabledOperations: Set<ScimOperation>? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var deleteAction: ScimDeleteAction = ScimDeleteAction.DEACTIVATE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    open var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT

    @Column(nullable = false)
    open var enabled: Boolean = true

    open var createdOn: OffsetDateTime? = null

    open var updatedOn: OffsetDateTime? = null
}
