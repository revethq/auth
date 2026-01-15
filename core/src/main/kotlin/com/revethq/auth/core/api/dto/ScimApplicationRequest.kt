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

import com.revethq.auth.core.scim.ScimDeleteAction
import com.revethq.auth.core.scim.ScimOperation
import jakarta.json.bind.annotation.JsonbNillable
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * Request DTO for creating or updating a SCIM application.
 */
@JsonbNillable(false)
data class ScimApplicationRequest(
    /**
     * The authorization server this SCIM application belongs to.
     */
    @field:NotNull
    var authorizationServerId: UUID? = null,

    /**
     * Optional Application ID for JWT-based authentication.
     * If not provided and autoCreateApplication=true, an Application will be auto-created.
     */
    var applicationId: UUID? = null,

    /**
     * Human-readable name for this SCIM application.
     */
    @field:NotBlank
    var name: String? = null,

    /**
     * Base URL of the downstream SCIM service provider.
     * Example: https://api.example.com/scim/v2
     */
    @field:NotBlank
    var baseUrl: String? = null,

    /**
     * Mapping of Revet Auth attributes to SCIM schema attributes.
     * Keys are SCIM attribute paths, values are JSONPath expressions.
     */
    var attributeMapping: Map<String, String>? = null,

    /**
     * Set of SCIM operations enabled for this application.
     * If not provided, all operations will be enabled.
     */
    var enabledOperations: Set<ScimOperation>? = null,

    /**
     * Action to take when a user is deleted.
     * Defaults to DEACTIVATE.
     */
    var deleteAction: ScimDeleteAction? = null,

    /**
     * Retry policy configuration.
     */
    var retryPolicy: RetryPolicyDto? = null,

    /**
     * Whether this SCIM application is enabled.
     */
    var enabled: Boolean? = true,

    /**
     * Whether to auto-create an Application if applicationId is not provided.
     * Defaults to true.
     */
    var autoCreateApplication: Boolean? = true
)

/**
 * DTO for retry policy configuration.
 */
@JsonbNillable(false)
data class RetryPolicyDto(
    var maxRetries: Int? = null,
    var initialBackoffMs: Long? = null,
    var maxBackoffMs: Long? = null,
    var backoffMultiplier: Double? = null
)
