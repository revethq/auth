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

import com.revethq.auth.core.scim.ScimProvisioningStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA entity for tracking SCIM delivery status.
 */
@Entity
@Table(
    name = "scim_delivery_status",
    indexes = [
        Index(name = "idx_scim_delivery_event", columnList = "eventId"),
        Index(name = "idx_scim_delivery_app", columnList = "scimApplicationId"),
        Index(name = "idx_scim_delivery_status_retry", columnList = "status, nextRetryAt"),
        Index(name = "idx_scim_delivery_created", columnList = "createdOn")
    ]
)
open class ScimDeliveryStatus {
    @Id
    @GeneratedValue
    open var id: UUID? = null

    @Column(nullable = false)
    open var eventId: UUID? = null

    @Column(nullable = false)
    open var scimApplicationId: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: ScimProvisioningStatus = ScimProvisioningStatus.PENDING

    /**
     * External ID returned by the downstream SCIM server.
     */
    open var scimResourceId: String? = null

    /**
     * HTTP status code from the last delivery attempt.
     */
    open var httpStatus: Int? = null

    @Column(nullable = false)
    open var retryCount: Int = 0

    /**
     * Scheduled time for next retry attempt.
     */
    open var nextRetryAt: OffsetDateTime? = null

    /**
     * Brief error message from the last failed attempt.
     */
    @Column(length = 1000)
    open var lastError: String? = null

    open var createdOn: OffsetDateTime? = null

    /**
     * When the delivery completed (successfully or exhausted retries).
     */
    open var completedOn: OffsetDateTime? = null
}
