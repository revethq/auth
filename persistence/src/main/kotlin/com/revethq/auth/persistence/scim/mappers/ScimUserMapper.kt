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
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import org.jboss.logging.Logger

/**
 * Maps Revet Auth user data to SCIM User schema.
 */
@ApplicationScoped
class ScimUserMapper {

    companion object {
        private val LOG = Logger.getLogger(ScimUserMapper::class.java)
        private val SCIM_USER_SCHEMA = listOf("urn:ietf:params:scim:schemas:core:2.0:User")
        private val SCIM_PATCH_OP_SCHEMA = listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp")

        // Default attribute mapping from Revet Auth to SCIM
        val DEFAULT_ATTRIBUTE_MAPPING = mapOf(
            "userName" to "$.username",
            "name.givenName" to "$.profile.given_name",
            "name.familyName" to "$.profile.family_name",
            "emails[0].value" to "$.email",
            "emails[0].primary" to "true",
            "active" to "$.active"
        )
    }

    private val jsonb: Jsonb = JsonbBuilder.create()

    /**
     * Maps user data to a SCIM User create/update request body.
     *
     * @param userData Map containing user and profile data from the Event resource
     * @param scimApplication SCIM application with attribute mapping configuration
     * @param externalId Optional external ID for updates
     * @return JSON string for SCIM request body
     */
    fun mapToScimUser(
        userData: Map<String, Any>,
        scimApplication: ScimApplication,
        externalId: String? = null
    ): String {
        val attributeMapping = scimApplication.attributeMapping ?: DEFAULT_ATTRIBUTE_MAPPING

        val scimUser = mutableMapOf<String, Any>(
            "schemas" to SCIM_USER_SCHEMA
        )

        if (externalId != null) {
            scimUser["id"] = externalId
        }

        // Apply attribute mapping
        for ((scimPath, sourcePath) in attributeMapping) {
            val value = extractValue(userData, sourcePath)
            if (value != null) {
                setNestedValue(scimUser, scimPath, value)
            }
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
     * Extracts a value from the user data using a simple JSONPath-like expression.
     * Supports paths like: $.username, $.profile.given_name, $.email
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

                // Ensure array has enough elements
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
