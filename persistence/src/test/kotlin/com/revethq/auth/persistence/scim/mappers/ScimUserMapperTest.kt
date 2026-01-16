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

package com.revethq.auth.persistence.scim.mappers

import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.domain.User
import jakarta.json.bind.JsonbBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class ScimUserMapperTest {

    private lateinit var mapper: ScimUserMapper
    private val jsonb = JsonbBuilder.create()

    @BeforeEach
    fun setUp() {
        mapper = ScimUserMapper()
    }

    private fun createScimApplication(
        attributeMapping: Map<String, String>? = null
    ): ScimApplication {
        return ScimApplication(
            id = UUID.randomUUID(),
            authorizationServerId = UUID.randomUUID(),
            applicationId = UUID.randomUUID(),
            name = "Test SCIM App",
            baseUrl = "https://api.example.com/scim/v2",
            attributeMapping = attributeMapping
        )
    }

    // =========================================================================
    // Domain Object Mapping Tests (Preferred API)
    // =========================================================================

    @Nested
    inner class DomainObjectMapping {

        @Test
        fun `maps User with default mapping`() {
            val user = User(
                id = UUID.randomUUID(),
                username = "jdoe",
                email = "jdoe@example.com"
            )
            val scimApp = createScimApplication()

            val json = mapper.mapToScimUser(user, null, scimApp)
            val result = jsonb.fromJson(json, Map::class.java)

            assertEquals("jdoe", result["userName"])
            assertEquals(user.id.toString(), result["externalId"])

            @Suppress("UNCHECKED_CAST")
            val emails = result["emails"] as List<Map<String, Any>>
            assertEquals(1, emails.size)
            assertEquals("jdoe@example.com", emails[0]["value"])
            assertEquals(true, emails[0]["primary"])
        }

        @Test
        fun `maps User and Profile with default mapping`() {
            val user = User(
                id = UUID.randomUUID(),
                username = "jdoe",
                email = "jdoe@example.com"
            )
            val profile = Profile(
                profile = mapOf(
                    "given_name" to "John",
                    "family_name" to "Doe"
                )
            )
            val scimApp = createScimApplication()

            val json = mapper.mapToScimUser(user, profile, scimApp)
            val result = jsonb.fromJson(json, Map::class.java)

            assertEquals("jdoe", result["userName"])

            @Suppress("UNCHECKED_CAST")
            val name = result["name"] as Map<String, Any>
            assertEquals("John", name["givenName"])
            assertEquals("Doe", name["familyName"])
        }

        @Test
        fun `maps User with custom attribute mapping`() {
            val user = User(
                id = UUID.randomUUID(),
                username = "jdoe",
                email = "jdoe@example.com"
            )
            val customMapping = mapOf(
                "userName" to "$.user.username",
                "emails[0].value" to "$.user.email",
                "emails[0].primary" to "true"
            )
            val scimApp = createScimApplication(attributeMapping = customMapping)

            val json = mapper.mapToScimUser(user, null, scimApp)
            val result = jsonb.fromJson(json, Map::class.java)

            assertEquals("jdoe", result["userName"])

            @Suppress("UNCHECKED_CAST")
            val emails = result["emails"] as List<Map<String, Any>>
            assertEquals("jdoe@example.com", emails[0]["value"])
        }

        @Test
        fun `includes SCIM resource ID when provided`() {
            val user = User(username = "jdoe")
            val scimApp = createScimApplication()
            val scimResourceId = "scim-user-123"

            val json = mapper.mapToScimUser(user, null, scimApp, scimResourceId)
            val result = jsonb.fromJson(json, Map::class.java)

            assertEquals(scimResourceId, result["id"])
        }

        @Test
        fun `includes SCIM schemas`() {
            val user = User(username = "jdoe")
            val scimApp = createScimApplication()

            val json = mapper.mapToScimUser(user, null, scimApp)
            val result = jsonb.fromJson(json, Map::class.java)

            @Suppress("UNCHECKED_CAST")
            val schemas = result["schemas"] as List<String>
            assertTrue(schemas.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
        }

        @Test
        fun `handles User with missing optional fields`() {
            val user = User(username = "jdoe")
            val scimApp = createScimApplication()

            val json = mapper.mapToScimUser(user, null, scimApp)
            val result = jsonb.fromJson(json, Map::class.java)

            assertEquals("jdoe", result["userName"])
            assertNotNull(result["schemas"])
        }
    }

    // =========================================================================
    // Deactivation PATCH Tests
    // =========================================================================

    @Test
    fun `mapToDeactivatePatch creates correct PATCH operation`() {
        val json = mapper.mapToDeactivatePatch()
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val schemas = result["schemas"] as List<String>
        assertTrue(schemas.contains("urn:ietf:params:scim:api:messages:2.0:PatchOp"))

        @Suppress("UNCHECKED_CAST")
        val operations = result["Operations"] as List<Map<String, Any>>
        assertEquals(1, operations.size)
        assertEquals("replace", operations[0]["op"])
        assertEquals("active", operations[0]["path"])
        assertEquals(false, operations[0]["value"])
    }
}
