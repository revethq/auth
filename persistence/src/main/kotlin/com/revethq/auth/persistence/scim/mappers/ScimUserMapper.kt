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
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder

/**
 * Maps Revet Auth User/Profile domain objects to SCIM User schema.
 */
@ApplicationScoped
class ScimUserMapper {

    companion object {
        private val SCIM_USER_SCHEMA = listOf("urn:ietf:params:scim:schemas:core:2.0:User")
        private val SCIM_PATCH_OP_SCHEMA = listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp")
    }

    private val jsonb: Jsonb = JsonbBuilder.create()

    /**
     * Maps User and Profile domain objects to a SCIM User request body.
     *
     * @param user User domain object
     * @param profile Optional Profile domain object
     * @param scimApplication SCIM application with optional custom attribute mapping
     * @param scimResourceId Optional SCIM resource ID for updates
     * @return JSON string for SCIM request body
     */
    fun mapToScimUser(
        user: User,
        profile: Profile? = null,
        scimApplication: ScimApplication,
        scimResourceId: String? = null
    ): String {
        val scimUser = mutableMapOf<String, Any>(
            "schemas" to SCIM_USER_SCHEMA
        )

        if (scimResourceId != null) {
            scimUser["id"] = scimResourceId
        }

        // If custom attribute mapping is configured, use JSONPath extraction
        if (scimApplication.attributeMapping != null) {
            val userData = buildUserDataMap(user, profile)
            applyAttributeMapping(userData, scimApplication.attributeMapping!!, scimUser)
        } else {
            // Use direct mapping for default case (more efficient)
            applyDefaultMapping(user, profile, scimUser)
        }

        return jsonb.toJson(scimUser)
    }

    /**
     * Creates a SCIM PATCH request to deactivate a user (set active=false).
     */
    fun mapToDeactivatePatch(): String {
        val patchRequest = mapOf(
            "schemas" to SCIM_PATCH_OP_SCHEMA,
            "Operations" to listOf(
                mapOf(
                    "op" to "replace",
                    "path" to "active",
                    "value" to false
                )
            )
        )
        return jsonb.toJson(patchRequest)
    }

    /**
     * Applies default mapping directly from domain objects.
     */
    private fun applyDefaultMapping(user: User, profile: Profile?, scimUser: MutableMap<String, Any>) {
        // userName (required)
        user.username?.let { scimUser["userName"] = it }

        // externalId - use the Revet Auth user ID
        user.id?.let { scimUser["externalId"] = it.toString() }

        // emails
        user.email?.let { email ->
            scimUser["emails"] = listOf(
                mapOf(
                    "value" to email,
                    "primary" to true
                )
            )
        }

        // name (from profile)
        val profileData = profile?.profile
        if (profileData != null) {
            val name = mutableMapOf<String, Any>()
            profileData["given_name"]?.let { name["givenName"] = it }
            profileData["family_name"]?.let { name["familyName"] = it }
            if (name.isNotEmpty()) {
                scimUser["name"] = name
            }
        }
    }

    /**
     * Builds a user data map from domain objects for custom attribute mapping.
     * Structure: { "user": {...}, "profile": {...} }
     */
    private fun buildUserDataMap(user: User, profile: Profile?): Map<String, Any> {
        val userMap = mutableMapOf<String, Any>()
        user.id?.let { userMap["id"] = it.toString() }
        user.username?.let { userMap["username"] = it }
        user.email?.let { userMap["email"] = it }

        val result = mutableMapOf<String, Any>("user" to userMap)
        profile?.profile?.let { result["profile"] = it }

        return result
    }

    /**
     * Applies custom attribute mapping using JSONPath extraction.
     */
    private fun applyAttributeMapping(
        data: Map<String, Any>,
        attributeMapping: Map<String, String>,
        scimUser: MutableMap<String, Any>
    ) {
        for ((scimPath, sourcePath) in attributeMapping) {
            val value = extractValue(data, sourcePath)
            if (value != null) {
                setNestedValue(scimUser, scimPath, value)
            }
        }
    }

    /**
     * Extracts a value from the user data using a simple JSONPath-like expression.
     * Supports paths like: $.user.username, $.profile.given_name
     */
    private fun extractValue(data: Map<String, Any>, path: String): Any? {
        // Handle literal values
        if (path == "true") return true
        if (path == "false") return false
        if (!path.startsWith("$.")) return path

        val pathParts = path.removePrefix("$.").split(".")
        var current: Any? = data

        for (part in pathParts) {
            current = when (current) {
                is Map<*, *> -> current[part]
                else -> null
            }
            if (current == null) break
        }

        return current
    }

    /**
     * Sets a value at a nested path in the SCIM user map.
     * Supports paths like: userName, name.givenName, emails[0].value
     */
    private fun setNestedValue(map: MutableMap<String, Any>, path: String, value: Any) {
        val parts = path.split(".")
        var current = map

        for (i in 0 until parts.size - 1) {
            val part = parts[i]

            // Handle array notation like "emails[0]"
            val arrayMatch = """(\w+)\[(\d+)\]""".toRegex().matchEntire(part)
            if (arrayMatch != null) {
                val arrayName = arrayMatch.groupValues[1]
                val index = arrayMatch.groupValues[2].toInt()

                @Suppress("UNCHECKED_CAST")
                val array = current.getOrPut(arrayName) { mutableListOf<MutableMap<String, Any>>() } as MutableList<MutableMap<String, Any>>

                while (array.size <= index) {
                    array.add(mutableMapOf())
                }

                current = array[index]
            } else {
                @Suppress("UNCHECKED_CAST")
                current = current.getOrPut(part) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
            }
        }

        val lastPart = parts.last()

        // Handle array notation in last part
        val arrayMatch = """(\w+)\[(\d+)\]""".toRegex().matchEntire(lastPart)
        if (arrayMatch != null) {
            val arrayName = arrayMatch.groupValues[1]
            val index = arrayMatch.groupValues[2].toInt()

            @Suppress("UNCHECKED_CAST")
            val array = current.getOrPut(arrayName) { mutableListOf<MutableMap<String, Any>>() } as MutableList<Any>

            while (array.size <= index) {
                array.add(mutableMapOf<String, Any>())
            }
            array[index] = value
        } else {
            current[lastPart] = value
        }
    }
}
