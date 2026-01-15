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

import com.revethq.auth.core.domain.Scope
import com.revethq.auth.core.scim.ScimOperation
import java.util.UUID

/**
 * Service for managing SCIM-related scopes.
 */
interface ScimScopeService {

    /**
     * Ensure all SCIM scopes exist for the given authorization server.
     * Creates any missing scopes.
     *
     * @param authorizationServerId The authorization server ID
     * @return List of all SCIM scopes (newly created and existing)
     */
    fun ensureScimScopes(authorizationServerId: UUID): List<Scope>

    /**
     * Get existing SCIM scopes for an authorization server.
     */
    fun getScimScopes(authorizationServerId: UUID): List<Scope>

    /**
     * Get the required scopes for a set of SCIM operations.
     *
     * @param authorizationServerId The authorization server ID
     * @param operations The enabled SCIM operations
     * @return List of required Scope entities
     */
    fun getScopesForOperations(authorizationServerId: UUID, operations: Set<ScimOperation>): List<Scope>

    /**
     * Validate that an application has the required scopes for the given operations.
     *
     * @param applicationId The application ID to validate
     * @param operations The operations that require scope validation
     * @return true if the application has all required scopes
     */
    fun validateApplicationScopes(applicationId: UUID, operations: Set<ScimOperation>): Boolean
}
