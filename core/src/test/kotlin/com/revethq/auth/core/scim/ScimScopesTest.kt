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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScimScopesTest {

    @Test
    fun `scope constants have correct values`() {
        assertEquals("scim:users:read", ScimScopes.USERS_READ)
        assertEquals("scim:users:write", ScimScopes.USERS_WRITE)
        assertEquals("scim:groups:read", ScimScopes.GROUPS_READ)
        assertEquals("scim:groups:write", ScimScopes.GROUPS_WRITE)
    }

    @Test
    fun `ALL_SCOPES contains all scope constants`() {
        val allScopes = ScimScopes.ALL_SCOPES

        assertEquals(4, allScopes.size)
        assertTrue(allScopes.contains(ScimScopes.USERS_READ))
        assertTrue(allScopes.contains(ScimScopes.USERS_WRITE))
        assertTrue(allScopes.contains(ScimScopes.GROUPS_READ))
        assertTrue(allScopes.contains(ScimScopes.GROUPS_WRITE))
    }

    @Test
    fun `SCOPE_DESCRIPTIONS has descriptions for all scopes`() {
        val descriptions = ScimScopes.SCOPE_DESCRIPTIONS

        assertEquals(4, descriptions.size)
        assertTrue(descriptions.containsKey(ScimScopes.USERS_READ))
        assertTrue(descriptions.containsKey(ScimScopes.USERS_WRITE))
        assertTrue(descriptions.containsKey(ScimScopes.GROUPS_READ))
        assertTrue(descriptions.containsKey(ScimScopes.GROUPS_WRITE))
    }

    @Test
    fun `user operations map to USERS_WRITE scope`() {
        assertEquals(ScimScopes.USERS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.CREATE_USER])
        assertEquals(ScimScopes.USERS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.UPDATE_USER])
        assertEquals(ScimScopes.USERS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.DEACTIVATE_USER])
        assertEquals(ScimScopes.USERS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.DELETE_USER])
    }

    @Test
    fun `group operations map to GROUPS_WRITE scope`() {
        assertEquals(ScimScopes.GROUPS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.CREATE_GROUP])
        assertEquals(ScimScopes.GROUPS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.UPDATE_GROUP])
        assertEquals(ScimScopes.GROUPS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.DELETE_GROUP])
        assertEquals(ScimScopes.GROUPS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.ADD_GROUP_MEMBER])
        assertEquals(ScimScopes.GROUPS_WRITE, ScimScopes.OPERATION_TO_SCOPE[ScimOperation.REMOVE_GROUP_MEMBER])
    }

    @Test
    fun `getRequiredScopes returns only USERS_WRITE for user operations`() {
        val operations = setOf(
            ScimOperation.CREATE_USER,
            ScimOperation.UPDATE_USER,
            ScimOperation.DELETE_USER
        )

        val requiredScopes = ScimScopes.getRequiredScopes(operations)

        assertEquals(setOf(ScimScopes.USERS_WRITE), requiredScopes)
    }

    @Test
    fun `getRequiredScopes returns only GROUPS_WRITE for group operations`() {
        val operations = setOf(
            ScimOperation.CREATE_GROUP,
            ScimOperation.UPDATE_GROUP,
            ScimOperation.ADD_GROUP_MEMBER
        )

        val requiredScopes = ScimScopes.getRequiredScopes(operations)

        assertEquals(setOf(ScimScopes.GROUPS_WRITE), requiredScopes)
    }

    @Test
    fun `getRequiredScopes returns both scopes for mixed operations`() {
        val operations = setOf(
            ScimOperation.CREATE_USER,
            ScimOperation.CREATE_GROUP
        )

        val requiredScopes = ScimScopes.getRequiredScopes(operations)

        assertEquals(setOf(ScimScopes.USERS_WRITE, ScimScopes.GROUPS_WRITE), requiredScopes)
    }

    @Test
    fun `getRequiredScopes returns empty set for empty operations`() {
        val requiredScopes = ScimScopes.getRequiredScopes(emptySet())

        assertTrue(requiredScopes.isEmpty())
    }

    @Test
    fun `hasRequiredScopes returns true when all required scopes are granted`() {
        val grantedScopes = setOf(ScimScopes.USERS_WRITE, ScimScopes.GROUPS_WRITE)
        val operations = setOf(ScimOperation.CREATE_USER, ScimOperation.CREATE_GROUP)

        assertTrue(ScimScopes.hasRequiredScopes(grantedScopes, operations))
    }

    @Test
    fun `hasRequiredScopes returns true when extra scopes are granted`() {
        val grantedScopes = setOf(
            ScimScopes.USERS_READ,
            ScimScopes.USERS_WRITE,
            ScimScopes.GROUPS_READ,
            ScimScopes.GROUPS_WRITE
        )
        val operations = setOf(ScimOperation.CREATE_USER)

        assertTrue(ScimScopes.hasRequiredScopes(grantedScopes, operations))
    }

    @Test
    fun `hasRequiredScopes returns false when required scope is missing`() {
        val grantedScopes = setOf(ScimScopes.USERS_WRITE)
        val operations = setOf(ScimOperation.CREATE_USER, ScimOperation.CREATE_GROUP)

        assertFalse(ScimScopes.hasRequiredScopes(grantedScopes, operations))
    }

    @Test
    fun `hasRequiredScopes returns true for empty operations`() {
        val grantedScopes = emptySet<String>()
        val operations = emptySet<ScimOperation>()

        assertTrue(ScimScopes.hasRequiredScopes(grantedScopes, operations))
    }
}
