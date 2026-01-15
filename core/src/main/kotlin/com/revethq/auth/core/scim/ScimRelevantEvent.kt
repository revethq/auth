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

package com.revethq.auth.core.scim

import com.revethq.auth.core.domain.ResourceType
import java.time.OffsetDateTime
import java.util.UUID

/**
 * CDI event fired when a SCIM-relevant event occurs (User, Group, GroupMember changes).
 * This event is fired after the Event is persisted to the database, allowing
 * ScimEventProcessor implementations to observe and process the event.
 */
data class ScimRelevantEvent(
    /**
     * The ID of the persisted Event.
     */
    val eventId: UUID,

    /**
     * The type of resource that changed.
     */
    val resourceType: ResourceType,

    /**
     * The ID of the resource that changed.
     */
    val resourceId: UUID,

    /**
     * The authorization server ID the resource belongs to.
     */
    val authorizationServerId: UUID,

    /**
     * The type of event (CREATE, UPDATE, DELETE).
     */
    val eventType: String,

    /**
     * When the event occurred.
     */
    val occurredAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {
        /**
         * Resource types that are relevant for SCIM provisioning.
         */
        val SCIM_RELEVANT_TYPES = setOf(
            ResourceType.USER,
            ResourceType.GROUP,
            ResourceType.GROUP_MEMBER
        )

        /**
         * Check if a resource type is SCIM-relevant.
         */
        fun isScimRelevant(resourceType: ResourceType): Boolean =
            resourceType in SCIM_RELEVANT_TYPES
    }
}
