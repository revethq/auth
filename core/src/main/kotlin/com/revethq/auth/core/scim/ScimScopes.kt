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

package com.revethq.auth.core.scim

/**
 * SCIM OAuth scope constants following industry patterns.
 * These scopes control access to SCIM resources and are included in JWTs
 * used for outbound SCIM requests.
 */
object ScimScopes {
    /**
     * Query SCIM user resources (GET /Users, GET /Users/{id}).
     */
    const val USERS_READ = "scim:users:read"

    /**
     * Create, update, and delete SCIM users.
     */
    const val USERS_WRITE = "scim:users:write"

    /**
     * Query SCIM group resources (GET /Groups, GET /Groups/{id}).
     */
    const val GROUPS_READ = "scim:groups:read"

    /**
     * Create, update, and delete SCIM groups.
     */
    const val GROUPS_WRITE = "scim:groups:write"

    /**
     * All SCIM scope names.
     */
    val ALL_SCOPES = listOf(USERS_READ, USERS_WRITE, GROUPS_READ, GROUPS_WRITE)

    /**
     * Scope descriptions for use when creating Scope entities.
     */
    val SCOPE_DESCRIPTIONS = mapOf(
        USERS_READ to "Query SCIM user resources",
        USERS_WRITE to "Create, update, and delete SCIM users",
        GROUPS_READ to "Query SCIM group resources",
        GROUPS_WRITE to "Create, update, and delete SCIM groups"
    )

    /**
     * Maps SCIM operations to the required scope.
     */
    val OPERATION_TO_SCOPE: Map<ScimOperation, String> = mapOf(
        ScimOperation.CREATE_USER to USERS_WRITE,
        ScimOperation.UPDATE_USER to USERS_WRITE,
        ScimOperation.DEACTIVATE_USER to USERS_WRITE,
        ScimOperation.DELETE_USER to USERS_WRITE,
        ScimOperation.CREATE_GROUP to GROUPS_WRITE,
        ScimOperation.UPDATE_GROUP to GROUPS_WRITE,
        ScimOperation.DELETE_GROUP to GROUPS_WRITE,
        ScimOperation.ADD_GROUP_MEMBER to GROUPS_WRITE,
        ScimOperation.REMOVE_GROUP_MEMBER to GROUPS_WRITE
    )

    /**
     * Get the required scopes for a set of operations.
     */
    fun getRequiredScopes(operations: Set<ScimOperation>): Set<String> =
        operations.mapNotNull { OPERATION_TO_SCOPE[it] }.toSet()

    /**
     * Check if the given scopes are sufficient for the specified operations.
     */
    fun hasRequiredScopes(grantedScopes: Set<String>, operations: Set<ScimOperation>): Boolean {
        val requiredScopes = getRequiredScopes(operations)
        return grantedScopes.containsAll(requiredScopes)
    }
}
