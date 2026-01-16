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

import com.revethq.auth.core.scim.RetryPolicy
import com.revethq.auth.core.scim.ScimDeleteAction
import com.revethq.auth.core.scim.ScimOperation
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Configuration for a downstream SCIM service provider.
 * Each ScimApplication represents a target system that will receive
 * user and group provisioning events from Revet Auth.
 */
data class ScimApplication(
    var id: UUID? = null,

    /**
     * The authorization server this SCIM application belongs to.
     */
    var authorizationServerId: UUID? = null,

    /**
     * Link to the Application entity used for JWT-based authentication.
     * The Application's credentials are used to generate tokens for SCIM requests.
     */
    var applicationId: UUID? = null,

    /**
     * Human-readable name for this SCIM application.
     */
    var name: String? = null,

    /**
     * Base URL of the downstream SCIM service provider.
     * Example: https://api.example.com/scim/v2
     */
    var baseUrl: String? = null,

    /**
     * Mapping of Revet Auth attributes to SCIM schema attributes.
     * Keys are SCIM attribute paths, values are JSONPath expressions.
     * Example: {"userName": "$.username", "name.givenName": "$.profile.given_name"}
     */
    var attributeMapping: Map<String, String>? = null,

    /**
     * Set of SCIM operations enabled for this application.
     */
    var enabledOperations: Set<ScimOperation>? = null,

    /**
     * Action to take when a user is deleted in Revet Auth.
     * Default is DEACTIVATE (set active=false) following Okta's pattern.
     */
    var deleteAction: ScimDeleteAction = ScimDeleteAction.DEACTIVATE,

    /**
     * Retry policy configuration for failed deliveries.
     */
    var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,

    /**
     * Whether this SCIM application is currently enabled.
     */
    var enabled: Boolean = true,

    var createdOn: OffsetDateTime? = null,
    var updatedOn: OffsetDateTime? = null
) {
    companion object {
        /**
         * Default attribute mapping from Revet Auth User/Profile to SCIM Core User schema.
         * Data structure: { "user": {...}, "profile": {...} }
         */
        val DEFAULT_USER_ATTRIBUTE_MAPPING = mapOf(
            "userName" to "$.user.username",
            "externalId" to "$.user.id",
            "name.givenName" to "$.profile.given_name",
            "name.familyName" to "$.profile.family_name",
            "emails[0].value" to "$.user.email",
            "emails[0].primary" to "true"
        )

        /**
         * Default attribute mapping for SCIM Group schema.
         * Data structure: { "group": {...} }
         */
        val DEFAULT_GROUP_ATTRIBUTE_MAPPING = mapOf(
            "displayName" to "$.group.displayName",
            "externalId" to "$.group.id"
        )
    }
}
