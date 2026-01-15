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

package com.revethq.auth.core.api.dto

import com.revethq.auth.core.scim.ScimProvisioningStatus
import jakarta.json.bind.annotation.JsonbNillable
import jakarta.json.bind.annotation.JsonbProperty
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Response DTO for a SCIM delivery status record.
 */
@JsonbNillable(false)
data class ScimDeliveryStatusResponse(
    @field:JsonbProperty("id")
    var id: UUID? = null,

    @field:JsonbProperty("eventId")
    var eventId: UUID? = null,

    @field:JsonbProperty("scimApplicationId")
    var scimApplicationId: UUID? = null,

    @field:JsonbProperty("status")
    var status: ScimProvisioningStatus? = null,

    @field:JsonbProperty("scimResourceId")
    var scimResourceId: String? = null,

    @field:JsonbProperty("httpStatus")
    var httpStatus: Int? = null,

    @field:JsonbProperty("retryCount")
    var retryCount: Int? = null,

    @field:JsonbProperty("nextRetryAt")
    var nextRetryAt: OffsetDateTime? = null,

    @field:JsonbProperty("lastError")
    var lastError: String? = null,

    @field:JsonbProperty("createdOn")
    var createdOn: OffsetDateTime? = null,

    @field:JsonbProperty("completedOn")
    var completedOn: OffsetDateTime? = null
)

/**
 * Response DTO for listing SCIM delivery statuses.
 */
@JsonbNillable(false)
data class ScimDeliveryStatusesResponse(
    @field:JsonbProperty("deliveryStatuses")
    var deliveryStatuses: List<ScimDeliveryStatusResponse>? = null,

    @field:JsonbProperty("page")
    var page: Page? = null
)
