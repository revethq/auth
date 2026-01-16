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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.revethq.auth.core.domain.AuthorizationServer
import com.revethq.auth.core.domain.Group
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.domain.User
import com.revethq.auth.core.services.AuthorizationServerService
import com.revethq.auth.core.services.ScimApplicationService
import com.revethq.auth.core.services.ScimScopeService
import com.revethq.auth.persistence.scim.operations.ScimGroupOperations
import com.revethq.auth.persistence.scim.operations.ScimUserOperations
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.UUID

/**
 * Integration tests for SCIM HTTP operations using WireMock.
 *
 * Tests 14.10-14.12: Verify HTTP requests are correctly sent to SCIM endpoints.
 *
 * Note: Event creation and CDI event tests are in ScimEventIntegrationTest (persistence module).
 */
@QuarkusTest
class ScimProvisioningIntegrationTest {

    @Inject
    lateinit var scimApplicationService: ScimApplicationService

    @Inject
    lateinit var authorizationServerService: AuthorizationServerService

    @Inject
    lateinit var scimScopeService: ScimScopeService

    @Inject
    lateinit var scimUserOperations: ScimUserOperations

    @Inject
    lateinit var scimGroupOperations: ScimGroupOperations

    private lateinit var wireMockServer: WireMockServer
    private var authServerId: UUID? = null

    @BeforeEach
    fun setUp() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val authServer = AuthorizationServer(
            name = "SCIM Test Auth Server ${UUID.randomUUID()}",
            serverUrl = URI("https://auth.test.com").toURL(),
            audience = "test-audience"
        )
        val created = authorizationServerService.createAuthorizationServer(authServer)
        authServerId = created.id!!
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    // =========================================================================
    // User Operations Tests
    // =========================================================================

    @Test
    fun `createUser sends POST to Users endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimUserEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val user = User(
            id = UUID.randomUUID(),
            username = "newuser",
            email = "newuser@example.com"
        )
        val profile = Profile(
            profile = mapOf(
                "given_name" to "New",
                "family_name" to "User"
            )
        )

        val response = scimUserOperations.createUser(scimApp, token, user, profile)

        assertTrue(response.isSuccess)
        assertEquals(scimResourceId, response.scimResourceId)

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/Users"))
                .withHeader("Authorization", containing("Bearer "))
                .withHeader("Content-Type", containing("application/scim+json"))
        )
    }

    @Test
    fun `updateUser sends PUT to Users endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimUserUpdateEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val user = User(
            id = UUID.randomUUID(),
            username = "updateduser",
            email = "updated@example.com"
        )

        val response = scimUserOperations.updateUser(scimApp, token, scimResourceId, user)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            putRequestedFor(urlPathEqualTo("/Users/$scimResourceId"))
                .withHeader("Authorization", containing("Bearer "))
        )
    }

    @Test
    fun `deactivateUser sends PATCH to Users endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimUserPatchEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val response = scimUserOperations.deactivateUser(scimApp, token, scimResourceId)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            patchRequestedFor(urlPathEqualTo("/Users/$scimResourceId"))
                .withHeader("Authorization", containing("Bearer "))
        )
    }

    @Test
    fun `deleteUser sends DELETE to Users endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimUserDeleteEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val response = scimUserOperations.deleteUser(scimApp, token, scimResourceId)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            deleteRequestedFor(urlPathEqualTo("/Users/$scimResourceId"))
                .withHeader("Authorization", containing("Bearer "))
        )
    }

    // =========================================================================
    // Group Operations Tests
    // =========================================================================

    @Test
    fun `createGroup sends POST to Groups endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimGroupEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val group = Group(
            id = UUID.randomUUID(),
            displayName = "New Group"
        )

        val response = scimGroupOperations.createGroup(scimApp, token, group)

        assertTrue(response.isSuccess)
        assertEquals(scimResourceId, response.scimResourceId)

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/Groups"))
                .withHeader("Authorization", containing("Bearer "))
                .withHeader("Content-Type", containing("application/scim+json"))
        )
    }

    @Test
    fun `updateGroup sends PUT to Groups endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimGroupUpdateEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val group = Group(
            id = UUID.randomUUID(),
            displayName = "Updated Group"
        )

        val response = scimGroupOperations.updateGroup(scimApp, token, scimResourceId, group)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            putRequestedFor(urlPathEqualTo("/Groups/$scimResourceId"))
        )
    }

    @Test
    fun `deleteGroup sends DELETE to Groups endpoint`() {
        val scimResourceId = UUID.randomUUID().toString()
        setupMockScimGroupDeleteEndpoint(scimResourceId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val response = scimGroupOperations.deleteGroup(scimApp, token, scimResourceId)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            deleteRequestedFor(urlPathEqualTo("/Groups/$scimResourceId"))
        )
    }

    @Test
    fun `addMember sends PATCH to Groups endpoint`() {
        val groupScimId = UUID.randomUUID().toString()
        val userScimId = UUID.randomUUID().toString()
        setupMockScimGroupPatchEndpoint(groupScimId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val response = scimGroupOperations.addMember(scimApp, token, groupScimId, userScimId)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            patchRequestedFor(urlPathEqualTo("/Groups/$groupScimId"))
                .withHeader("Authorization", containing("Bearer "))
        )
    }

    @Test
    fun `removeMember sends PATCH to Groups endpoint`() {
        val groupScimId = UUID.randomUUID().toString()
        val userScimId = UUID.randomUUID().toString()
        setupMockScimGroupPatchEndpoint(groupScimId)

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val response = scimGroupOperations.removeMember(scimApp, token, groupScimId, userScimId)

        assertTrue(response.isSuccess)

        wireMockServer.verify(
            patchRequestedFor(urlPathEqualTo("/Groups/$groupScimId"))
        )
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    @Test
    fun `HTTP 500 response returns retryable error`() {
        setupMockScimUserEndpoint(statusCode = 500, errorMessage = "Internal Server Error")

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val user = User(username = "testuser", email = "test@example.com")
        val response = scimUserOperations.createUser(scimApp, token, user)

        assertTrue(!response.isSuccess)
        assertTrue(response.isRetryable)
        assertEquals(500, response.statusCode)
    }

    @Test
    fun `HTTP 400 response returns non-retryable error`() {
        setupMockScimUserEndpoint(statusCode = 400, errorMessage = "Bad Request")

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val user = User(username = "testuser", email = "test@example.com")
        val response = scimUserOperations.createUser(scimApp, token, user)

        assertTrue(!response.isSuccess)
        assertTrue(!response.isRetryable)
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `HTTP 401 response returns non-retryable error`() {
        setupMockScimUserEndpoint(statusCode = 401, errorMessage = "Unauthorized")

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val user = User(username = "testuser", email = "test@example.com")
        val response = scimUserOperations.createUser(scimApp, token, user)

        assertTrue(!response.isSuccess)
        assertEquals(401, response.statusCode)
    }

    @Test
    fun `HTTP 503 response returns retryable error`() {
        setupMockScimUserEndpoint(statusCode = 503, errorMessage = "Service Unavailable")

        val scimApp = createScimApplication()
        val token = generateToken(scimApp)

        val user = User(username = "testuser", email = "test@example.com")
        val response = scimUserOperations.createUser(scimApp, token, user)

        assertTrue(!response.isSuccess)
        assertTrue(response.isRetryable)
        assertEquals(503, response.statusCode)
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun createScimApplication(): ScimApplication {
        val baseUrl = "http://localhost:${wireMockServer.port()}"
        val scimApp = ScimApplication(
            authorizationServerId = authServerId,
            name = "Test SCIM App ${UUID.randomUUID()}",
            baseUrl = baseUrl
            // enabledOperations defaults to all operations when null
        )
        val result = scimApplicationService.createScimApplication(scimApp, autoCreateApplication = true)
        // Verify the applicationId was assigned
        requireNotNull(result.scimApplication.applicationId) { "applicationId should be assigned after creation" }
        return result.scimApplication
    }

    private fun setupMockScimUserEndpoint(
        scimResourceId: String = UUID.randomUUID().toString(),
        statusCode: Int = 201,
        errorMessage: String? = null
    ) {
        val responseBody = if (errorMessage != null) {
            """{"detail": "$errorMessage"}"""
        } else {
            """
            {
                "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                "id": "$scimResourceId",
                "userName": "testuser",
                "active": true
            }
            """.trimIndent()
        }

        wireMockServer.stubFor(
            post(urlPathEqualTo("/Users"))
                .willReturn(
                    aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/scim+json")
                        .withBody(responseBody)
                )
        )
    }

    private fun setupMockScimUserUpdateEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            put(urlPathEqualTo("/Users/$scimResourceId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/scim+json")
                        .withBody("""
                            {
                                "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                                "id": "$scimResourceId",
                                "userName": "testuser",
                                "active": true
                            }
                        """.trimIndent())
                )
        )
    }

    private fun setupMockScimUserPatchEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            patch(urlPathEqualTo("/Users/$scimResourceId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/scim+json")
                        .withBody("""
                            {
                                "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                                "id": "$scimResourceId",
                                "userName": "testuser",
                                "active": false
                            }
                        """.trimIndent())
                )
        )
    }

    private fun setupMockScimUserDeleteEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            delete(urlPathEqualTo("/Users/$scimResourceId"))
                .willReturn(aResponse().withStatus(204))
        )
    }

    private fun setupMockScimGroupEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/Groups"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/scim+json")
                        .withBody("""
                            {
                                "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                                "id": "$scimResourceId",
                                "displayName": "Test Group"
                            }
                        """.trimIndent())
                )
        )
    }

    private fun setupMockScimGroupUpdateEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            put(urlPathEqualTo("/Groups/$scimResourceId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/scim+json")
                        .withBody("""
                            {
                                "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                                "id": "$scimResourceId",
                                "displayName": "Updated Group"
                            }
                        """.trimIndent())
                )
        )
    }

    private fun setupMockScimGroupDeleteEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            delete(urlPathEqualTo("/Groups/$scimResourceId"))
                .willReturn(aResponse().withStatus(204))
        )
    }

    private fun setupMockScimGroupPatchEndpoint(scimResourceId: String) {
        wireMockServer.stubFor(
            patch(urlPathEqualTo("/Groups/$scimResourceId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/scim+json")
                        .withBody("""
                            {
                                "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                                "id": "$scimResourceId",
                                "displayName": "Test Group"
                            }
                        """.trimIndent())
                )
        )
    }

    private fun generateToken(scimApp: ScimApplication): String {
        // Get all SCIM scopes for this authorization server
        val scopes = scimScopeService.getScimScopes(scimApp.authorizationServerId!!)
        val accessToken = authorizationServerService.generateClientCredentialsAccessToken(
            authorizationServerId = scimApp.authorizationServerId!!,
            applicationId = scimApp.applicationId!!,
            subject = scimApp.applicationId.toString(),
            scopes = scopes,
            expiresInSeconds = 3600
        )
        return accessToken.accessToken!!
    }
}
