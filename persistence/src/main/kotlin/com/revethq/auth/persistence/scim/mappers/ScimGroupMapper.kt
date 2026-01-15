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
 * Maps Revet Auth group data to SCIM Group schema.
 */
@ApplicationScoped
class ScimGroupMapper {

    companion object {
        private val LOG = Logger.getLogger(ScimGroupMapper::class.java)
        private val SCIM_GROUP_SCHEMA = listOf("urn:ietf:params:scim:schemas:core:2.0:Group")
        private val SCIM_PATCH_OP_SCHEMA = listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp")

        // Default attribute mapping for groups
        val DEFAULT_ATTRIBUTE_MAPPING = mapOf(
            "displayName" to "$.name"
        )
    }

    private val jsonb: Jsonb = JsonbBuilder.create()

    /**
     * Maps group data to a SCIM Group create/update request body.
     *
     * @param groupData Map containing group data from the Event resource
     * @param scimApplication SCIM application with attribute mapping configuration
     * @param externalId Optional external ID for updates
     * @return JSON string for SCIM request body
     */
    fun mapToScimGroup(
        groupData: Map<String, Any>,
        scimApplication: ScimApplication,
        externalId: String? = null
    ): String {
        val scimGroup = mutableMapOf<String, Any>(
            "schemas" to SCIM_GROUP_SCHEMA
        )

        if (externalId != null) {
            scimGroup["id"] = externalId
        }

        // Extract displayName from group data
        val groupName = extractValue(groupData, "$.name")
        if (groupName != null) {
            scimGroup["displayName"] = groupName
        }

        return jsonb.toJson(scimGroup)
    }

    /**
     * Creates a SCIM PATCH request to add a member to a group.
     *
     * @param memberScimId The SCIM ID of the user to add
     * @return JSON string for SCIM PATCH request body
     */
    fun mapToAddMemberPatch(memberScimId: String): String {
        val patchRequest = mapOf(
            "schemas" to SCIM_PATCH_OP_SCHEMA,
            "Operations" to listOf(
                mapOf(
                    "op" to "add",
                    "path" to "members",
                    "value" to listOf(
                        mapOf("value" to memberScimId)
                    )
                )
            )
        )
        return jsonb.toJson(patchRequest)
    }

    /**
     * Creates a SCIM PATCH request to remove a member from a group.
     *
     * @param memberScimId The SCIM ID of the user to remove
     * @return JSON string for SCIM PATCH request body
     */
    fun mapToRemoveMemberPatch(memberScimId: String): String {
        val patchRequest = mapOf(
            "schemas" to SCIM_PATCH_OP_SCHEMA,
            "Operations" to listOf(
                mapOf(
                    "op" to "remove",
                    "path" to "members[value eq \"$memberScimId\"]"
                )
            )
        )
        return jsonb.toJson(patchRequest)
    }

    /**
     * Extracts a value from the group data using a simple JSONPath-like expression.
     */
    private fun extractValue(data: Map<String, Any>, path: String): Any? {
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
}
