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

import com.revethq.auth.core.domain.ScimApplication
import jakarta.json.bind.JsonbBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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

    @Test
    fun `mapToScimUser with default mapping extracts basic user fields`() {
        val userData = mapOf(
            "username" to "jdoe",
            "email" to "jdoe@example.com",
            "active" to true,
            "profile" to mapOf(
                "given_name" to "John",
                "family_name" to "Doe"
            )
        )
        val scimApp = createScimApplication()

        val json = mapper.mapToScimUser(userData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        assertEquals("jdoe", result["userName"])
        assertEquals(true, result["active"])

        @Suppress("UNCHECKED_CAST")
        val name = result["name"] as Map<String, Any>
        assertEquals("John", name["givenName"])
        assertEquals("Doe", name["familyName"])

        @Suppress("UNCHECKED_CAST")
        val emails = result["emails"] as List<Map<String, Any>>
        assertEquals(1, emails.size)
        assertEquals("jdoe@example.com", emails[0]["value"])
        assertEquals(true, emails[0]["primary"])
    }

    @Test
    fun `mapToScimUser includes SCIM schemas`() {
        val userData = mapOf("username" to "jdoe")
        val scimApp = createScimApplication()

        val json = mapper.mapToScimUser(userData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val schemas = result["schemas"] as List<String>
        assertTrue(schemas.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
    }

    @Test
    fun `mapToScimUser includes external ID when provided`() {
        val userData = mapOf("username" to "jdoe")
        val scimApp = createScimApplication()
        val externalId = "scim-user-123"

        val json = mapper.mapToScimUser(userData, scimApp, externalId)
        val result = jsonb.fromJson(json, Map::class.java)

        assertEquals(externalId, result["id"])
    }

    @Test
    fun `mapToScimUser with custom attribute mapping`() {
        val userData = mapOf(
            "login" to "johnd",
            "emailAddress" to "john@company.com",
            "firstName" to "John",
            "lastName" to "Doe"
        )
        val customMapping = mapOf(
            "userName" to "$.login",
            "emails[0].value" to "$.emailAddress",
            "name.givenName" to "$.firstName",
            "name.familyName" to "$.lastName"
        )
        val scimApp = createScimApplication(attributeMapping = customMapping)

        val json = mapper.mapToScimUser(userData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        assertEquals("johnd", result["userName"])

        @Suppress("UNCHECKED_CAST")
        val emails = result["emails"] as List<Map<String, Any>>
        assertEquals("john@company.com", emails[0]["value"])

        @Suppress("UNCHECKED_CAST")
        val name = result["name"] as Map<String, Any>
        assertEquals("John", name["givenName"])
        assertEquals("Doe", name["familyName"])
    }

    @Test
    fun `mapToScimUser handles missing fields gracefully`() {
        val userData = mapOf(
            "username" to "jdoe"
            // Missing email, profile, active
        )
        val scimApp = createScimApplication()

        val json = mapper.mapToScimUser(userData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        assertEquals("jdoe", result["userName"])
        // Should not throw, missing fields are simply not included
        assertNotNull(result["schemas"])
    }

    @Test
    fun `mapToScimUser handles nested profile data`() {
        val userData = mapOf(
            "username" to "jdoe",
            "profile" to mapOf(
                "given_name" to "John",
                "family_name" to "Doe",
                "middle_name" to "William"
            )
        )
        val scimApp = createScimApplication()

        val json = mapper.mapToScimUser(userData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val name = result["name"] as Map<String, Any>
        assertEquals("John", name["givenName"])
        assertEquals("Doe", name["familyName"])
    }

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

    @Test
    fun `mapToScimUser with literal boolean values in mapping`() {
        val userData = mapOf("username" to "jdoe")
        val customMapping = mapOf(
            "userName" to "$.username",
            "emails[0].primary" to "true",
            "active" to "true"
        )
        val scimApp = createScimApplication(attributeMapping = customMapping)

        val json = mapper.mapToScimUser(userData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val emails = result["emails"] as List<Map<String, Any>>
        assertEquals(true, emails[0]["primary"])
        assertEquals(true, result["active"])
    }
}
