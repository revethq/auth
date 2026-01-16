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

package com.revethq.auth.web.scim

import com.revethq.auth.core.domain.Application
import com.revethq.auth.core.domain.AuthorizationServer
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.exceptions.badrequests.ScimApplicationInvalidScopes
import com.revethq.auth.core.scim.ScimOperation
import com.revethq.auth.core.scim.ScimScopes
import com.revethq.auth.core.services.ApplicationService
import com.revethq.auth.core.services.AuthorizationServerService
import com.revethq.auth.core.services.ScimApplicationService
import com.revethq.auth.core.services.ScimScopeService
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.UUID

/**
 * Integration tests for ScimApplicationService.
 * Tests 14.2 - Application auto-creation with scopes
 * Tests 14.3 - Scope validation on existing Application
 */
@QuarkusTest
class ScimApplicationServiceTest {

    @Inject
    lateinit var scimApplicationService: ScimApplicationService

    @Inject
    lateinit var authorizationServerService: AuthorizationServerService

    @Inject
    lateinit var applicationService: ApplicationService

    @Inject
    lateinit var scimScopeService: ScimScopeService

    private var authServerId: UUID? = null

    @BeforeEach
    fun setUp() {
        val authServer = AuthorizationServer(
            name = "Test Auth Server ${UUID.randomUUID()}",
            serverUrl = URI("https://auth.test.com").toURL(),
            audience = "test-audience"
        )
        val created = authorizationServerService.createAuthorizationServer(authServer)
        authServerId = created.id!!
    }

    // =========================================================================
    // 14.2 - Application auto-creation with scopes
    // =========================================================================

    @Test
    fun `auto-create application creates Application with SCIM scopes`() {
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "Auto-Created App",
            baseUrl = "https://api.example.com/scim/v2"
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)

        assertNotNull(result.scimApplication.applicationId)
        assertNotNull(result.applicationSecret)
        assertNotNull(result.applicationSecret?.applicationSecret)
    }

    @Test
    fun `auto-create application assigns correct scopes for user operations`() {
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "User Only App",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(ScimOperation.CREATE_USER, ScimOperation.UPDATE_USER)
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)

        // The application should have the USERS_WRITE scope
        val applicationId = result.scimApplication.applicationId
        assertNotNull(applicationId)

        // Verify the application has the correct scopes by checking scope validation passes
        val isValid = scimScopeService.validateApplicationScopes(
            applicationId!!,
            setOf(ScimOperation.CREATE_USER, ScimOperation.UPDATE_USER)
        )
        assertTrue(isValid)
    }

    @Test
    fun `auto-create application assigns scopes for group operations`() {
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "Group Only App",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(ScimOperation.CREATE_GROUP, ScimOperation.UPDATE_GROUP)
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)

        val applicationId = result.scimApplication.applicationId
        assertNotNull(applicationId)

        val isValid = scimScopeService.validateApplicationScopes(
            applicationId!!,
            setOf(ScimOperation.CREATE_GROUP, ScimOperation.UPDATE_GROUP)
        )
        assertTrue(isValid)
    }

    @Test
    fun `auto-create application assigns both user and group scopes for mixed operations`() {
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "Mixed Operations App",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(
                ScimOperation.CREATE_USER,
                ScimOperation.CREATE_GROUP,
                ScimOperation.ADD_GROUP_MEMBER
            )
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)

        val applicationId = result.scimApplication.applicationId
        assertNotNull(applicationId)

        // Should have both USERS_WRITE and GROUPS_WRITE scopes
        val isValidForUsers = scimScopeService.validateApplicationScopes(
            applicationId!!,
            setOf(ScimOperation.CREATE_USER)
        )
        val isValidForGroups = scimScopeService.validateApplicationScopes(
            applicationId,
            setOf(ScimOperation.CREATE_GROUP)
        )
        assertTrue(isValidForUsers)
        assertTrue(isValidForGroups)
    }

    @Test
    fun `auto-create application generates unique client credentials`() {
        val scimApp1 = ScimApplication(
            authorizationServerId = authServerId,
            name = "App One",
            baseUrl = "https://api.example.com/scim/v2"
        )
        val scimApp2 = ScimApplication(
            authorizationServerId = authServerId,
            name = "App Two",
            baseUrl = "https://api.example.com/scim/v2"
        )

        val result1 = scimApplicationService.createScimApplication(scimApp1, autoCreateApplication = true)
        val result2 = scimApplicationService.createScimApplication(scimApp2, autoCreateApplication = true)

        // Different applications should have different secrets
        assertNotNull(result1.applicationSecret?.id)
        assertNotNull(result2.applicationSecret?.id)
        assertTrue(result1.applicationSecret?.id != result2.applicationSecret?.id)
    }

    @Test
    fun `auto-create with no operations defaults to all operations`() {
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "Default Ops App",
            baseUrl = "https://api.example.com/scim/v2"
            // enabledOperations not specified
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)

        // Should have all operations enabled
        assertNotNull(result.scimApplication.enabledOperations)
        assertEquals(ScimOperation.entries.toSet(), result.scimApplication.enabledOperations)
    }

    // =========================================================================
    // 14.3 - Scope validation on existing Application
    // =========================================================================

    @Test
    fun `create with existing application validates scopes`() {
        // First ensure SCIM scopes exist
        scimScopeService.ensureScimScopes(authServerId!!)

        // Create an application with the required scopes
        val scopes = scimScopeService.getScopesForOperations(
            authServerId!!,
            setOf(ScimOperation.CREATE_USER)
        )

        val application = Application(
            authorizationServerId = authServerId,
            clientId = "existing-client-${UUID.randomUUID()}",
            name = "Existing Application",
            scopes = scopes
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val createdApp = applicationService.createApplication(application, profile)
        val applicationId = createdApp.left!!.id!!

        // Create SCIM application using existing application
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            applicationId = applicationId,
            name = "Using Existing App",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(ScimOperation.CREATE_USER)
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = false)

        assertEquals(applicationId, result.scimApplication.applicationId)
        // No new credentials should be returned
        assertEquals(null, result.applicationSecret)
    }

    @Test
    fun `create with existing application throws when scopes are missing`() {
        // Create an application WITHOUT SCIM scopes
        val application = Application(
            authorizationServerId = authServerId,
            clientId = "no-scopes-client-${UUID.randomUUID()}",
            name = "No Scopes Application",
            scopes = emptyList()
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val createdApp = applicationService.createApplication(application, profile)
        val applicationId = createdApp.left!!.id!!

        // Try to create SCIM application - should fail due to missing scopes
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            applicationId = applicationId,
            name = "Should Fail",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(ScimOperation.CREATE_USER)
        )

        assertThrows(ScimApplicationInvalidScopes::class.java) {
            scimApplicationService.createScimApplication(scimApp, autoCreateApplication = false)
        }
    }

    @Test
    fun `update validates scopes when operations change`() {
        // Create SCIM app with auto-created application (users only)
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "User Only Initially",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(ScimOperation.CREATE_USER)
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)

        // Now try to update to include group operations
        // This should fail because the auto-created app only has USERS_WRITE scope
        val updatedScimApp = result.scimApplication.copy(
            enabledOperations = setOf(ScimOperation.CREATE_USER, ScimOperation.CREATE_GROUP)
        )

        assertThrows(ScimApplicationInvalidScopes::class.java) {
            scimApplicationService.updateScimApplication(updatedScimApp)
        }
    }

    @Test
    fun `scope validation fails when application lacks GROUPS_WRITE`() {
        // Ensure SCIM scopes exist
        scimScopeService.ensureScimScopes(authServerId!!)

        // Create application with only USERS_WRITE scope
        val userScopes = scimScopeService.getScopesForOperations(
            authServerId!!,
            setOf(ScimOperation.CREATE_USER)
        )

        val application = Application(
            authorizationServerId = authServerId,
            clientId = "users-only-${UUID.randomUUID()}",
            name = "Users Only App",
            scopes = userScopes
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val createdApp = applicationService.createApplication(application, profile)
        val applicationId = createdApp.left!!.id!!

        // Try to create SCIM app requiring group operations
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            applicationId = applicationId,
            name = "Needs Groups Scope",
            baseUrl = "https://api.example.com/scim/v2",
            enabledOperations = setOf(ScimOperation.CREATE_GROUP)
        )

        val exception = assertThrows(ScimApplicationInvalidScopes::class.java) {
            scimApplicationService.createScimApplication(scimApp, autoCreateApplication = false)
        }

        assertTrue(exception.message?.contains(ScimScopes.GROUPS_WRITE) == true)
    }

    @Test
    fun `scope validation passes when application has all required scopes`() {
        // Ensure SCIM scopes exist
        scimScopeService.ensureScimScopes(authServerId!!)

        // Create application with all SCIM scopes
        val allScopes = scimScopeService.getScimScopes(authServerId!!)

        val application = Application(
            authorizationServerId = authServerId,
            clientId = "all-scopes-${UUID.randomUUID()}",
            name = "All Scopes App",
            scopes = allScopes
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val createdApp = applicationService.createApplication(application, profile)
        val applicationId = createdApp.left!!.id!!

        // Should be able to create SCIM app with any operations
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            applicationId = applicationId,
            name = "Full Operations",
            baseUrl = "https://api.example.com/scim/v2"
            // Default to all operations
        )

        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = false)

        assertNotNull(result.scimApplication.id)
        assertEquals(applicationId, result.scimApplication.applicationId)
    }
}
