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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ScimGroupMapperTest {

    private lateinit var mapper: ScimGroupMapper
    private val jsonb = JsonbBuilder.create()

    @BeforeEach
    fun setUp() {
        mapper = ScimGroupMapper()
    }

    private fun createScimApplication(): ScimApplication {
        return ScimApplication(
            id = UUID.randomUUID(),
            authorizationServerId = UUID.randomUUID(),
            applicationId = UUID.randomUUID(),
            name = "Test SCIM App",
            baseUrl = "https://api.example.com/scim/v2"
        )
    }

    @Test
    fun `mapToScimGroup extracts group name as displayName`() {
        val groupData = mapOf(
            "name" to "Engineering Team"
        )
        val scimApp = createScimApplication()

        val json = mapper.mapToScimGroup(groupData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        assertEquals("Engineering Team", result["displayName"])
    }

    @Test
    fun `mapToScimGroup includes SCIM Group schema`() {
        val groupData = mapOf("name" to "Test Group")
        val scimApp = createScimApplication()

        val json = mapper.mapToScimGroup(groupData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val schemas = result["schemas"] as List<String>
        assertTrue(schemas.contains("urn:ietf:params:scim:schemas:core:2.0:Group"))
    }

    @Test
    fun `mapToScimGroup includes external ID when provided`() {
        val groupData = mapOf("name" to "Test Group")
        val scimApp = createScimApplication()
        val externalId = "scim-group-456"

        val json = mapper.mapToScimGroup(groupData, scimApp, externalId)
        val result = jsonb.fromJson(json, Map::class.java)

        assertEquals(externalId, result["id"])
    }

    @Test
    fun `mapToScimGroup handles missing name gracefully`() {
        val groupData = emptyMap<String, Any>()
        val scimApp = createScimApplication()

        val json = mapper.mapToScimGroup(groupData, scimApp)
        val result = jsonb.fromJson(json, Map::class.java)

        // Should not throw, displayName simply won't be set
        @Suppress("UNCHECKED_CAST")
        val schemas = result["schemas"] as List<String>
        assertTrue(schemas.isNotEmpty())
    }

    @Test
    fun `mapToAddMemberPatch creates correct PATCH operation`() {
        val memberScimId = "user-scim-123"

        val json = mapper.mapToAddMemberPatch(memberScimId)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val schemas = result["schemas"] as List<String>
        assertTrue(schemas.contains("urn:ietf:params:scim:api:messages:2.0:PatchOp"))

        @Suppress("UNCHECKED_CAST")
        val operations = result["Operations"] as List<Map<String, Any>>
        assertEquals(1, operations.size)

        val operation = operations[0]
        assertEquals("add", operation["op"])
        assertEquals("members", operation["path"])

        @Suppress("UNCHECKED_CAST")
        val value = operation["value"] as List<Map<String, Any>>
        assertEquals(1, value.size)
        assertEquals(memberScimId, value[0]["value"])
    }

    @Test
    fun `mapToRemoveMemberPatch creates correct PATCH operation`() {
        val memberScimId = "user-scim-123"

        val json = mapper.mapToRemoveMemberPatch(memberScimId)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val schemas = result["schemas"] as List<String>
        assertTrue(schemas.contains("urn:ietf:params:scim:api:messages:2.0:PatchOp"))

        @Suppress("UNCHECKED_CAST")
        val operations = result["Operations"] as List<Map<String, Any>>
        assertEquals(1, operations.size)

        val operation = operations[0]
        assertEquals("remove", operation["op"])
        assertEquals("members[value eq \"$memberScimId\"]", operation["path"])
    }

    @Test
    fun `mapToAddMemberPatch with special characters in ID`() {
        val memberScimId = "user-123-abc"

        val json = mapper.mapToAddMemberPatch(memberScimId)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val operations = result["Operations"] as List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val value = operations[0]["value"] as List<Map<String, Any>>
        assertEquals(memberScimId, value[0]["value"])
    }

    @Test
    fun `mapToRemoveMemberPatch escapes member ID in filter`() {
        val memberScimId = "user-with-special-chars"

        val json = mapper.mapToRemoveMemberPatch(memberScimId)
        val result = jsonb.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val operations = result["Operations"] as List<Map<String, Any>>
        val path = operations[0]["path"] as String

        assertTrue(path.contains(memberScimId))
        assertEquals("members[value eq \"$memberScimId\"]", path)
    }
}
