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

package com.revethq.auth.persistence.scim.operations

import com.revethq.auth.core.domain.Group
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.persistence.scim.client.ScimClient
import com.revethq.auth.persistence.scim.client.ScimClientResponse
import com.revethq.auth.persistence.scim.mappers.ScimGroupMapper
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

/**
 * SCIM operations for Group resources.
 */
@ApplicationScoped
class ScimGroupOperations(
    private val scimClient: ScimClient,
    private val scimGroupMapper: ScimGroupMapper
) {

    companion object {
        private val LOG = Logger.getLogger(ScimGroupOperations::class.java)
        private const val GROUPS_PATH = "/Groups"
    }

    /**
     * Create a group in the downstream SCIM server.
     * POST /Groups
     */
    fun createGroup(
        scimApplication: ScimApplication,
        token: String,
        group: Group
    ): ScimClientResponse {
        LOG.debug("Creating SCIM group for application ${scimApplication.id}")

        val body = scimGroupMapper.mapToScimGroup(group, scimApplication)
        return scimClient.createResource(scimApplication, token, GROUPS_PATH, body)
    }

    /**
     * Update a group in the downstream SCIM server.
     * PUT /Groups/{id}
     */
    fun updateGroup(
        scimApplication: ScimApplication,
        token: String,
        scimResourceId: String,
        group: Group
    ): ScimClientResponse {
        LOG.debug("Updating SCIM group $scimResourceId for application ${scimApplication.id}")

        val body = scimGroupMapper.mapToScimGroup(group, scimApplication, scimResourceId)
        return scimClient.replaceResource(scimApplication, token, GROUPS_PATH, scimResourceId, body)
    }

    /**
     * Delete a group from the downstream SCIM server.
     * DELETE /Groups/{id}
     */
    fun deleteGroup(
        scimApplication: ScimApplication,
        token: String,
        scimResourceId: String
    ): ScimClientResponse {
        LOG.debug("Deleting SCIM group $scimResourceId for application ${scimApplication.id}")

        return scimClient.deleteResource(scimApplication, token, GROUPS_PATH, scimResourceId)
    }

    /**
     * Add a member to a group.
     * PATCH /Groups/{id}
     */
    fun addMember(
        scimApplication: ScimApplication,
        token: String,
        groupScimId: String,
        memberScimId: String
    ): ScimClientResponse {
        LOG.debug("Adding member $memberScimId to SCIM group $groupScimId for application ${scimApplication.id}")

        val body = scimGroupMapper.mapToAddMemberPatch(memberScimId)
        return scimClient.patchResource(scimApplication, token, GROUPS_PATH, groupScimId, body)
    }

    /**
     * Remove a member from a group.
     * PATCH /Groups/{id}
     */
    fun removeMember(
        scimApplication: ScimApplication,
        token: String,
        groupScimId: String,
        memberScimId: String
    ): ScimClientResponse {
        LOG.debug("Removing member $memberScimId from SCIM group $groupScimId for application ${scimApplication.id}")

        val body = scimGroupMapper.mapToRemoveMemberPatch(memberScimId)
        return scimClient.patchResource(scimApplication, token, GROUPS_PATH, groupScimId, body)
    }
}
