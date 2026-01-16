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

package com.revethq.auth.web.api.routes.mappers

import com.revethq.auth.core.api.dto.RetryPolicyDto
import com.revethq.auth.core.api.dto.ScimApplicationCredentials
import com.revethq.auth.core.api.dto.ScimApplicationRequest
import com.revethq.auth.core.api.dto.ScimApplicationResponse
import com.revethq.auth.core.api.dto.ScimDeliveryStatusResponse
import com.revethq.auth.core.domain.ApplicationSecret
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.domain.ScimDeliveryStatus
import com.revethq.auth.core.scim.RetryPolicy
import com.revethq.auth.core.scim.ScimDeleteAction
import com.revethq.auth.core.scim.ScimOperation

/**
 * Mapper for SCIM application DTOs.
 */
object ScimApplicationMapper {

    @JvmStatic
    fun from(request: ScimApplicationRequest): ScimApplication {
        return ScimApplication(
            authorizationServerId = request.authorizationServerId,
            applicationId = request.applicationId,
            name = request.name,
            baseUrl = request.baseUrl,
            attributeMapping = request.attributeMapping,
            enabledOperations = request.enabledOperations ?: ScimOperation.entries.toSet(),
            deleteAction = request.deleteAction ?: ScimDeleteAction.DEACTIVATE,
            retryPolicy = fromRetryPolicyDto(request.retryPolicy),
            enabled = request.enabled ?: true
        )
    }

    @JvmStatic
    fun toResponse(scimApplication: ScimApplication, credentials: ApplicationSecret? = null): ScimApplicationResponse {
        return ScimApplicationResponse(
            id = scimApplication.id,
            authorizationServerId = scimApplication.authorizationServerId,
            applicationId = scimApplication.applicationId,
            name = scimApplication.name,
            baseUrl = scimApplication.baseUrl,
            attributeMapping = scimApplication.attributeMapping,
            enabledOperations = scimApplication.enabledOperations,
            deleteAction = scimApplication.deleteAction,
            retryPolicy = toRetryPolicyDto(scimApplication.retryPolicy),
            enabled = scimApplication.enabled,
            createdOn = scimApplication.createdOn,
            updatedOn = scimApplication.updatedOn,
            credentials = credentials?.let {
                ScimApplicationCredentials(
                    applicationSecretId = it.id,
                    applicationSecret = it.applicationSecret
                )
            }
        )
    }

    @JvmStatic
    fun toDeliveryStatusResponse(status: ScimDeliveryStatus): ScimDeliveryStatusResponse {
        return ScimDeliveryStatusResponse(
            id = status.id,
            eventId = status.eventId,
            scimApplicationId = status.scimApplicationId,
            status = status.status,
            scimResourceId = status.scimResourceId,
            httpStatus = status.httpStatus,
            retryCount = status.retryCount,
            nextRetryAt = status.nextRetryAt,
            lastError = status.lastError,
            createdOn = status.createdOn,
            completedOn = status.completedOn
        )
    }

    private fun fromRetryPolicyDto(dto: RetryPolicyDto?): RetryPolicy {
        if (dto == null) {
            return RetryPolicy.DEFAULT
        }
        return RetryPolicy(
            maxRetries = dto.maxRetries ?: RetryPolicy.DEFAULT.maxRetries,
            initialBackoffMs = dto.initialBackoffMs ?: RetryPolicy.DEFAULT.initialBackoffMs,
            maxBackoffMs = dto.maxBackoffMs ?: RetryPolicy.DEFAULT.maxBackoffMs,
            backoffMultiplier = dto.backoffMultiplier ?: RetryPolicy.DEFAULT.backoffMultiplier
        )
    }

    private fun toRetryPolicyDto(policy: RetryPolicy): RetryPolicyDto {
        return RetryPolicyDto(
            maxRetries = policy.maxRetries,
            initialBackoffMs = policy.initialBackoffMs,
            maxBackoffMs = policy.maxBackoffMs,
            backoffMultiplier = policy.backoffMultiplier
        )
    }
}
