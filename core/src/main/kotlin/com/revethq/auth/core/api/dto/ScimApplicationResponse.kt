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
import jakarta.json.bind.annotation.JsonbProperty
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Response DTO for a SCIM application.
 */
@JsonbNillable(false)
data class ScimApplicationResponse(
    @field:JsonbProperty("id")
    var id: UUID? = null,

    @field:JsonbProperty("authorizationServerId")
    var authorizationServerId: UUID? = null,

    @field:JsonbProperty("applicationId")
    var applicationId: UUID? = null,

    @field:JsonbProperty("name")
    var name: String? = null,

    @field:JsonbProperty("baseUrl")
    var baseUrl: String? = null,

    @field:JsonbProperty("attributeMapping")
    var attributeMapping: Map<String, String>? = null,

    @field:JsonbProperty("enabledOperations")
    var enabledOperations: Set<ScimOperation>? = null,

    @field:JsonbProperty("deleteAction")
    var deleteAction: ScimDeleteAction? = null,

    @field:JsonbProperty("retryPolicy")
    var retryPolicy: RetryPolicyDto? = null,

    @field:JsonbProperty("enabled")
    var enabled: Boolean? = null,

    @field:JsonbProperty("createdOn")
    var createdOn: OffsetDateTime? = null,

    @field:JsonbProperty("updatedOn")
    var updatedOn: OffsetDateTime? = null,

    /**
     * Application credentials returned only when an Application was auto-created.
     * This is the only time the raw secret value is available.
     */
    @field:JsonbProperty("credentials")
    var credentials: ScimApplicationCredentials? = null
)

/**
 * Credentials returned when an Application is auto-created for a SCIM application.
 */
@JsonbNillable(false)
data class ScimApplicationCredentials(
    @field:JsonbProperty("applicationSecretId")
    var applicationSecretId: UUID? = null,

    /**
     * The raw application secret. This is only returned once during creation.
     */
    @field:JsonbProperty("applicationSecret")
    var applicationSecret: String? = null
)

/**
 * Response DTO for listing SCIM applications.
 */
@JsonbNillable(false)
data class ScimApplicationsResponse(
    @field:JsonbProperty("scimApplications")
    var scimApplications: List<ScimApplicationResponse>? = null,

    @field:JsonbProperty("page")
    var page: Page? = null
)
