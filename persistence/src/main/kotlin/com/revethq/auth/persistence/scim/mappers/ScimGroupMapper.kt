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

import com.revethq.auth.core.domain.Group
import com.revethq.auth.core.domain.ScimApplication
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder

/**
 * Maps Revet Auth Group domain objects to SCIM Group schema.
 */
@ApplicationScoped
class ScimGroupMapper {

    companion object {
        private val SCIM_GROUP_SCHEMA = listOf("urn:ietf:params:scim:schemas:core:2.0:Group")
        private val SCIM_PATCH_OP_SCHEMA = listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp")
    }

    private val jsonb: Jsonb = JsonbBuilder.create()

    /**
     * Maps Group domain object to a SCIM Group request body.
     *
     * @param group Group domain object
     * @param scimApplication SCIM application (unused, for future custom attribute mapping)
     * @param scimResourceId Optional SCIM resource ID for updates
     * @return JSON string for SCIM request body
     */
    fun mapToScimGroup(
        group: Group,
        scimApplication: ScimApplication,
        scimResourceId: String? = null
    ): String {
        val scimGroup = mutableMapOf<String, Any>(
            "schemas" to SCIM_GROUP_SCHEMA
        )

        if (scimResourceId != null) {
            scimGroup["id"] = scimResourceId
        }

        // Direct mapping from domain object
        group.displayName?.let { scimGroup["displayName"] = it }
        group.id?.let { scimGroup["externalId"] = it.toString() }

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
}
