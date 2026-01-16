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

import com.revethq.auth.core.domain.AuthorizationServer
import com.revethq.auth.core.scim.ScimDeleteAction
import com.revethq.auth.core.scim.ScimOperation
import com.revethq.auth.core.services.AuthorizationServerService
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.UUID

@QuarkusTest
class ScimApplicationResourceTest {

    @Inject
    lateinit var authorizationServerService: AuthorizationServerService

    private var authServerId: UUID? = null

    @BeforeEach
    fun setUp() {
        // Create a test authorization server for each test
        val authServer = AuthorizationServer(
            name = "Test Auth Server ${UUID.randomUUID()}",
            serverUrl = URI("https://auth.test.com").toURL(),
            audience = "test-audience"
        )
        val created = authorizationServerService.createAuthorizationServer(authServer)
        authServerId = created.id!!
    }

    @Test
    fun `POST creates SCIM application with auto-provisioned credentials`() {
        val requestBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "Slack Integration",
                "baseUrl": "https://api.slack.com/scim/v2",
                "autoCreateApplication": true
            }
        """.trimIndent()

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .`when`()
            .post("/scim-applications")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("Slack Integration"))
            .body("baseUrl", equalTo("https://api.slack.com/scim/v2"))
            .body("authorizationServerId", equalTo(authServerId.toString()))
            .body("applicationId", notNullValue())
            .body("enabled", equalTo(true))
            .body("credentials.applicationSecretId", notNullValue())
            .body("credentials.applicationSecret", notNullValue())
    }

    @Test
    fun `POST creates SCIM application with custom operations`() {
        val requestBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "User-Only Sync",
                "baseUrl": "https://api.example.com/scim/v2",
                "enabledOperations": ["CREATE_USER", "UPDATE_USER", "DEACTIVATE_USER"],
                "autoCreateApplication": true
            }
        """.trimIndent()

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .`when`()
            .post("/scim-applications")
        .then()
            .statusCode(200)
            .body("enabledOperations", hasSize<Any>(3))
    }

    @Test
    fun `POST creates SCIM application with custom retry policy`() {
        val requestBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "Custom Retry App",
                "baseUrl": "https://api.example.com/scim/v2",
                "retryPolicy": {
                    "maxRetries": 10,
                    "initialBackoffMs": 2000,
                    "maxBackoffMs": 600000,
                    "backoffMultiplier": 3.0
                },
                "autoCreateApplication": true
            }
        """.trimIndent()

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .`when`()
            .post("/scim-applications")
        .then()
            .statusCode(200)
            .body("retryPolicy.maxRetries", equalTo(10))
            .body("retryPolicy.initialBackoffMs", equalTo(2000))
            .body("retryPolicy.backoffMultiplier", equalTo(3.0f))
    }

    @Test
    fun `POST creates SCIM application with DELETE action`() {
        val requestBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "Hard Delete App",
                "baseUrl": "https://api.example.com/scim/v2",
                "deleteAction": "DELETE",
                "autoCreateApplication": true
            }
        """.trimIndent()

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .`when`()
            .post("/scim-applications")
        .then()
            .statusCode(200)
            .body("deleteAction", equalTo("DELETE"))
    }

    @Test
    fun `GET lists SCIM applications`() {
        // Create two SCIM applications
        createScimApplication("App One")
        createScimApplication("App Two")

        given()
            .queryParam("authorizationServerIds", authServerId)
        .`when`()
            .get("/scim-applications")
        .then()
            .statusCode(200)
            .body("scimApplications", hasSize<Any>(2))
    }

    @Test
    fun `GET retrieves single SCIM application by ID`() {
        val scimAppId = createScimApplication("Test App")

        given()
        .`when`()
            .get("/scim-applications/$scimAppId")
        .then()
            .statusCode(200)
            .body("id", equalTo(scimAppId))
            .body("name", equalTo("Test App"))
            // Credentials should not be returned on GET (only on create)
            .body("credentials", nullValue())
    }

    @Test
    fun `GET returns 404 for non-existent SCIM application`() {
        val nonExistentId = UUID.randomUUID()

        given()
        .`when`()
            .get("/scim-applications/$nonExistentId")
        .then()
            .statusCode(404)
    }

    @Test
    fun `PUT updates SCIM application`() {
        val scimAppId = createScimApplication("Original Name")

        val updateBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "Updated Name",
                "baseUrl": "https://api.updated.com/scim/v2",
                "enabled": false
            }
        """.trimIndent()

        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
        .`when`()
            .put("/scim-applications/$scimAppId")
        .then()
            .statusCode(200)
            .body("name", equalTo("Updated Name"))
            .body("baseUrl", equalTo("https://api.updated.com/scim/v2"))
            .body("enabled", equalTo(false))
    }

    @Test
    fun `DELETE removes SCIM application`() {
        val scimAppId = createScimApplication("To Be Deleted")

        // Verify it exists
        given()
        .`when`()
            .get("/scim-applications/$scimAppId")
        .then()
            .statusCode(200)

        // Delete it
        given()
        .`when`()
            .delete("/scim-applications/$scimAppId")
        .then()
            .statusCode(204)

        // Verify it's gone
        given()
        .`when`()
            .get("/scim-applications/$scimAppId")
        .then()
            .statusCode(404)
    }

    @Test
    fun `GET delivery statuses returns empty list for new SCIM application`() {
        val scimAppId = createScimApplication("New App")

        given()
        .`when`()
            .get("/scim-applications/$scimAppId/deliveries")
        .then()
            .statusCode(200)
            .body("deliveryStatuses", hasSize<Any>(0))
    }

    @Test
    fun `POST with custom attribute mapping`() {
        val requestBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "Custom Mapping App",
                "baseUrl": "https://api.example.com/scim/v2",
                "attributeMapping": {
                    "userName": "$.login",
                    "emails[0].value": "$.emailAddress"
                },
                "autoCreateApplication": true
            }
        """.trimIndent()

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .`when`()
            .post("/scim-applications")
        .then()
            .statusCode(200)
            .body("attributeMapping.userName", equalTo("$.login"))
            .body("attributeMapping.'emails[0].value'", equalTo("$.emailAddress"))
    }

    private fun createScimApplication(name: String): String {
        val requestBody = """
            {
                "authorizationServerId": "$authServerId",
                "name": "$name",
                "baseUrl": "https://api.example.com/scim/v2",
                "autoCreateApplication": true
            }
        """.trimIndent()

        return given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .`when`()
            .post("/scim-applications")
        .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
    }
}
