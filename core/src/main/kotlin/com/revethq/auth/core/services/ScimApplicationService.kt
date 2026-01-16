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

package com.revethq.auth.core.services

import com.revethq.auth.core.domain.ApplicationSecret
import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.ScimApplication
import java.util.UUID

/**
 * Result of creating a SCIM application, including optional auto-created credentials.
 */
data class ScimApplicationCreateResult(
    val scimApplication: ScimApplication,
    /**
     * If an Application was auto-created, the application secret is returned.
     * This is the only time the raw secret value is available.
     */
    val applicationSecret: ApplicationSecret? = null
)

/**
 * Service for managing SCIM application configurations.
 */
interface ScimApplicationService {

    /**
     * Get all SCIM applications for the given authorization server IDs.
     */
    fun getScimApplications(authorizationServerIds: List<UUID>, page: Page): List<ScimApplication>

    /**
     * Get all enabled SCIM applications for a specific authorization server.
     */
    fun getEnabledScimApplications(authorizationServerId: UUID): List<ScimApplication>

    /**
     * Get a SCIM application by ID.
     */
    fun getScimApplication(scimApplicationId: UUID): ScimApplication

    /**
     * Create a new SCIM application.
     *
     * If applicationId is null in the request, an Application will be auto-created
     * with the necessary SCIM scopes, and the credentials will be returned in the result.
     *
     * If applicationId is provided, the existing Application will be validated
     * to ensure it has the required scopes for the enabled operations.
     *
     * @param scimApplication The SCIM application configuration
     * @param autoCreateApplication Whether to auto-create an Application if applicationId is null
     * @return The created SCIM application with optional credentials
     */
    fun createScimApplication(scimApplication: ScimApplication, autoCreateApplication: Boolean = true): ScimApplicationCreateResult

    /**
     * Update an existing SCIM application.
     */
    fun updateScimApplication(scimApplication: ScimApplication): ScimApplication

    /**
     * Delete a SCIM application by ID.
     * Note: This does not delete the associated Application entity.
     */
    fun deleteScimApplication(scimApplicationId: UUID)

    /**
     * Ensure SCIM scopes exist for the given authorization server.
     * Creates them if they don't already exist.
     */
    fun ensureScimScopes(authorizationServerId: UUID)
}
