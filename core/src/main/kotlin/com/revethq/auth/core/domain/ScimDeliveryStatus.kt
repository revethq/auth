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

package com.revethq.auth.core.domain

import com.revethq.auth.core.scim.ScimProvisioningStatus
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Tracks the status of delivering a specific event to a SCIM application.
 * This provides a lightweight audit trail that references the Event table
 * for full audit information.
 */
data class ScimDeliveryStatus(
    var id: UUID? = null,

    /**
     * Reference to the Event that triggered this delivery.
     * The Event contains the full audit trail including resource state.
     */
    var eventId: UUID? = null,

    /**
     * The SCIM application this delivery is targeting.
     */
    var scimApplicationId: UUID? = null,

    /**
     * Current status of the delivery.
     */
    var status: ScimProvisioningStatus = ScimProvisioningStatus.PENDING,

    /**
     * External ID returned by the downstream SCIM server after successful creation.
     * Used for subsequent UPDATE and DELETE operations.
     */
    var scimResourceId: String? = null,

    /**
     * HTTP status code from the last delivery attempt.
     */
    var httpStatus: Int? = null,

    /**
     * Number of retry attempts made.
     */
    var retryCount: Int = 0,

    /**
     * Scheduled time for next retry attempt.
     * Null when status is SUCCESS, FAILED, or PENDING (initial attempt).
     */
    var nextRetryAt: OffsetDateTime? = null,

    /**
     * Brief error message from the last failed attempt.
     */
    var lastError: String? = null,

    var createdOn: OffsetDateTime? = null,

    /**
     * When the delivery completed (successfully or exhausted retries).
     */
    var completedOn: OffsetDateTime? = null
)
